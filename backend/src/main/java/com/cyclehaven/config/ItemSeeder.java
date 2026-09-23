package com.cyclehaven.config;

import com.cyclehaven.entity.Category;
import com.cyclehaven.entity.Item;
import com.cyclehaven.repository.ItemRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * <p>Seeding matters more than it looks: a reviewer can clone the repo, start the
 * app, and immediately see a working storefront with products and images. The
 * original required importing a 12MB MySQL dump by hand before the site would
 * render anything at all.
 *
 * <p>Products are only inserted when the table is empty, so restarting never
 * duplicates the catalogue or overwrites an admin's edits.
 */
@Configuration
public class ItemSeeder {

    private static final Logger log = LoggerFactory.getLogger(ItemSeeder.class);

    /**
     * Image formats accepted for seed files, mapped to the content type they are
     * served with. Ordered: the first match for a given slug wins, so replacing
     * an illustration with a photograph is a matter of dropping in a .jpg — no
     * code change and no renaming.
     */
    private static final Map<String, String> IMAGE_FORMATS = new LinkedHashMap<>();

    static {
        IMAGE_FORMATS.put(".jpg", "image/jpeg");
        IMAGE_FORMATS.put(".jpeg", "image/jpeg");
        IMAGE_FORMATS.put(".webp", "image/webp");
        IMAGE_FORMATS.put(".png", "image/png");
    }

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

            int withImages = 0;

            for (SeedItem seed : seeds) {
                Item item = new Item();
                item.setName(seed.name());
                item.setCategory(Category.valueOf(seed.category()));
                item.setDescription(seed.description());
                item.setModel(seed.model());
                item.setPrice(BigDecimal.valueOf(seed.price()));
                item.setColour(seed.colour());
                item.setQuantity(seed.quantity());

                Optional<SeedImage> image = loadImage(seed.slug());
                if (image.isPresent()) {
                    item.setImageData(image.get().bytes());
                    item.setImageContentType(image.get().contentType());
                    withImages++;
                }

                itemRepository.save(item);
            }

            log.info("Seeded {} catalogue items ({} with images)", seeds.size(), withImages);
        };
    }

    /** Finds the first image matching this slug in any supported format. */
    private Optional<SeedImage> loadImage(String slug) {
        for (Map.Entry<String, String> format : IMAGE_FORMATS.entrySet()) {
            Resource resource = new ClassPathResource("seed/images/" + slug + format.getKey());
            if (!resource.exists()) {
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                return Optional.of(new SeedImage(StreamUtils.copyToByteArray(in), format.getValue()));
            } catch (IOException ex) {
                log.warn("Could not read seed image for '{}': {}", slug, ex.getMessage());
            }
        }
        log.warn("No seed image found for '{}'", slug);
        return Optional.empty();
    }

    private record SeedImage(byte[] bytes, String contentType) {
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
