package com.cyclehaven.config;

import com.cyclehaven.entity.Category;
import com.cyclehaven.entity.Item;
import com.cyclehaven.repository.ItemRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/**
 * Loads the starter catalogue on first run.
 *
 * <p>Seeding matters more than it looks: it means a reviewer can clone the repo,
 * start the app, and immediately see a working storefront with products and
 * images. The original required importing a 12MB MySQL dump by hand before the
 * site would render anything at all.
 *
 * <p>Products are only inserted when the table is empty, so restarting the
 * application never duplicates the catalogue or overwrites an admin's edits.
 */
@Configuration
public class ItemSeeder {

    private static final Logger log = LoggerFactory.getLogger(ItemSeeder.class);

    @Bean
    CommandLineRunner seedItems(
            ItemRepository itemRepository,
            ObjectMapper objectMapper,
            @Value("${cyclehaven.seed.enabled}") boolean seedEnabled) {

        return args -> {
            if (!seedEnabled || itemRepository.count() > 0) {
                return;
            }

            List<SeedItem> seeds;
            try (InputStream in = new ClassPathResource("seed/items.json").getInputStream()) {
                seeds = List.of(objectMapper.readValue(in, SeedItem[].class));
            }

            for (SeedItem seed : seeds) {
                Item item = new Item();
                item.setName(seed.name());
                item.setCategory(Category.valueOf(seed.category()));
                item.setDescription(seed.description());
                item.setModel(seed.model());
                item.setPrice(BigDecimal.valueOf(seed.price()));
                item.setColour(seed.colour());
                item.setQuantity(seed.quantity());
                loadImage(seed.slug()).ifPresent(bytes -> {
                    item.setImageData(bytes);
                    item.setImageContentType("image/png");
                });
                itemRepository.save(item);
            }

            log.info("Seeded {} catalogue items", seeds.size());
        };
    }

    private java.util.Optional<byte[]> loadImage(String slug) {
        Resource resource = new ClassPathResource("seed/images/" + slug + ".png");
        if (!resource.exists()) {
            log.warn("No seed image found for '{}'", slug);
            return java.util.Optional.empty();
        }
        try (InputStream in = resource.getInputStream()) {
            return java.util.Optional.of(StreamUtils.copyToByteArray(in));
        } catch (IOException ex) {
            log.warn("Could not read seed image for '{}': {}", slug, ex.getMessage());
            return java.util.Optional.empty();
        }
    }

    /** Shape of one entry in seed/items.json. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeedItem(
            String slug,
            String name,
            String category,
            String model,
            double price,
            String colour,
            int quantity,
            String description) {
    }
}
