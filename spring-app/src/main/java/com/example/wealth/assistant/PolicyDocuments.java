package com.example.wealth.assistant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PolicyDocuments {

    private static final Pattern SECTION_START = Pattern.compile("(?m)^(?=\\d+\\. )");

    @Bean
    VectorStore policyDocumentStore(EmbeddingModel embeddingModel, @Value("${wealth.data-dir}") String dataDir)
            throws IOException {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        store.add(sections(Path.of(dataDir, "documents")));
        return store;
    }

    // One chunk per numbered section: the documents are short and structured, so a section is a natural unit.
    // Each chunk starts with the document title so the model can tell which document it comes from.
    static List<Document> sections(Path documentsDir) throws IOException {
        List<Path> files;
        try (Stream<Path> listing = Files.list(documentsDir)) {
            files = listing.filter(file -> file.toString().endsWith(".txt")).sorted().toList();
        }
        List<Document> sections = new ArrayList<>();
        for (Path file : files) {
            String[] parts = SECTION_START.split(Files.readString(file));
            String title = parts[0].lines().findFirst().orElseThrow();
            for (int i = 1; i < parts.length; i++) {
                sections.add(new Document(title + "\n" + parts[i].strip(),
                        Map.of("source", file.getFileName().toString())));
            }
        }
        return sections;
    }
}
