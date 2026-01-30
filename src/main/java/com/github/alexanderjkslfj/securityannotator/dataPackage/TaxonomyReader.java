package com.github.alexanderjkslfj.securityannotator.dataPackage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TaxonomyReader {

    public String readTaxonomy() throws IOException {
        return new String(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/taxonomy/List.json")).readAllBytes(),
                StandardCharsets.UTF_8
        );
    }
}
