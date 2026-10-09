package com.example.wealth.assistant;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Startup
public class PolicyDocuments implements Supplier<RetrievalAugmentor> {

    private static final Pattern SECTION_START = Pattern.compile("(?m)^(?=\\d+\\. )");

    // Replaces LangChain4j's default injector text, with the same wording as the Spring app (ADR 0006).
    // {{userMessage}} and {{contents}} are filled in by DefaultContentInjector.
    static final PromptTemplate DOCUMENT_EXCERPTS_TEMPLATE = PromptTemplate.from("""
            {{userMessage}}

            Policy document excerpts that may be relevant, between the lines:
            ---------------------
            {{contents}}
            ---------------------

            Use these excerpts for what the bank's documents say, and name the document you rely on.
            For figures about the client's portfolio, use the tools; the excerpts contain no portfolio data.
            If neither the excerpts nor the tool results answer the question, say that you cannot answer it.
            """);

    private final RetrievalAugmentor retrievalAugmentor;

    public PolicyDocuments(EmbeddingModel embeddingModel, @ConfigProperty(name = "wealth.data-dir") String dataDir)
            throws IOException {
        List<TextSegment> sections = sections(Path.of(dataDir, "documents"));
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        store.addAll(embeddingModel.embedAll(sections).content(), sections);
        this.retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(EmbeddingStoreContentRetriever.builder()
                        .embeddingStore(store)
                        .embeddingModel(embeddingModel)
                        .maxResults(3)
                        .build())
                .contentInjector(DefaultContentInjector.builder().promptTemplate(DOCUMENT_EXCERPTS_TEMPLATE).build())
                .build();
    }

    @Override
    public RetrievalAugmentor get() {
        return retrievalAugmentor;
    }

    // One chunk per numbered section: the documents are short and structured, so a section is a natural unit.
    // Each chunk starts with the document title so the model can tell which document it comes from.
    static List<TextSegment> sections(Path documentsDir) throws IOException {
        List<Path> files;
        try (Stream<Path> listing = Files.list(documentsDir)) {
            files = listing.filter(file -> file.toString().endsWith(".txt")).sorted().toList();
        }
        List<TextSegment> sections = new ArrayList<>();
        for (Path file : files) {
            String[] parts = SECTION_START.split(Files.readString(file));
            String title = parts[0].lines().findFirst().orElseThrow();
            for (int i = 1; i < parts.length; i++) {
                sections.add(TextSegment.from(title + "\n" + parts[i].strip(),
                        Metadata.from("source", file.getFileName().toString())));
            }
        }
        return sections;
    }
}
