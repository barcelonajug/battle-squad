package org.barcelonajug.battlecontender.client;

public record AdvancedHeroSearchCriteria(
        String name,
        String alignment,
        String publisher,
        String role,
        String gender,
        String race,
        Integer minCost,
        Integer maxCost,
        Integer minPower,
        Integer maxPower,
        Integer minStrength,
        Integer maxStrength,
        Integer minSpeed,
        Integer maxSpeed,
        Integer minIntelligence,
        Integer maxIntelligence,
        Integer minDurability,
        Integer maxDurability,
        Integer minCombat,
        Integer maxCombat,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String alignment;
        private String publisher;
        private String role;
        private String gender;
        private String race;
        private Integer minCost;
        private Integer maxCost;
        private Integer minPower;
        private Integer maxPower;
        private Integer minStrength;
        private Integer maxStrength;
        private Integer minSpeed;
        private Integer maxSpeed;
        private Integer minIntelligence;
        private Integer maxIntelligence;
        private Integer minDurability;
        private Integer maxDurability;
        private Integer minCombat;
        private Integer maxCombat;
        private Integer page;
        private Integer size;
        private String sortBy;
        private String sortDirection;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder alignment(String alignment) {
            this.alignment = alignment;
            return this;
        }

        public Builder publisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder race(String race) {
            this.race = race;
            return this;
        }

        public Builder minCost(Integer minCost) {
            this.minCost = minCost;
            return this;
        }

        public Builder maxCost(Integer maxCost) {
            this.maxCost = maxCost;
            return this;
        }

        public Builder minPower(Integer minPower) {
            this.minPower = minPower;
            return this;
        }

        public Builder maxPower(Integer maxPower) {
            this.maxPower = maxPower;
            return this;
        }

        public Builder minStrength(Integer minStrength) {
            this.minStrength = minStrength;
            return this;
        }

        public Builder maxStrength(Integer maxStrength) {
            this.maxStrength = maxStrength;
            return this;
        }

        public Builder minSpeed(Integer minSpeed) {
            this.minSpeed = minSpeed;
            return this;
        }

        public Builder maxSpeed(Integer maxSpeed) {
            this.maxSpeed = maxSpeed;
            return this;
        }

        public Builder minIntelligence(Integer minIntelligence) {
            this.minIntelligence = minIntelligence;
            return this;
        }

        public Builder maxIntelligence(Integer maxIntelligence) {
            this.maxIntelligence = maxIntelligence;
            return this;
        }

        public Builder minDurability(Integer minDurability) {
            this.minDurability = minDurability;
            return this;
        }

        public Builder maxDurability(Integer maxDurability) {
            this.maxDurability = maxDurability;
            return this;
        }

        public Builder minCombat(Integer minCombat) {
            this.minCombat = minCombat;
            return this;
        }

        public Builder maxCombat(Integer maxCombat) {
            this.maxCombat = maxCombat;
            return this;
        }

        public Builder page(Integer page) {
            this.page = page;
            return this;
        }

        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        public Builder sortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder sortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }

        public AdvancedHeroSearchCriteria build() {
            return new AdvancedHeroSearchCriteria(
                    name,
                    alignment,
                    publisher,
                    role,
                    gender,
                    race,
                    minCost,
                    maxCost,
                    minPower,
                    maxPower,
                    minStrength,
                    maxStrength,
                    minSpeed,
                    maxSpeed,
                    minIntelligence,
                    maxIntelligence,
                    minDurability,
                    maxDurability,
                    minCombat,
                    maxCombat,
                    page,
                    size,
                    sortBy,
                    sortDirection);
        }
    }
}
