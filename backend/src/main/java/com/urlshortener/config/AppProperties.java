package com.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String baseUrl = "http://localhost:8080";
    private Security security = new Security();
    private Shortcode shortcode = new Shortcode();
    private Llm llm = new Llm();
    private Orchestration orchestration = new Orchestration();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Shortcode getShortcode() {
        return shortcode;
    }

    public void setShortcode(Shortcode shortcode) {
        this.shortcode = shortcode;
    }

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    public Orchestration getOrchestration() {
        return orchestration;
    }

    public void setOrchestration(Orchestration orchestration) {
        this.orchestration = orchestration;
    }

    public static class Security {
        private String apiKey = "local-dev-key";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Shortcode {
        private int length = 7;
        private int maxCollisionRetries = 5;

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getMaxCollisionRetries() {
            return maxCollisionRetries;
        }

        public void setMaxCollisionRetries(int maxCollisionRetries) {
            this.maxCollisionRetries = maxCollisionRetries;
        }
    }

    public static class Llm {
        private String provider = "stub";
        private OpenAi openai = new OpenAi();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public OpenAi getOpenai() {
            return openai;
        }

        public void setOpenai(OpenAi openai) {
            this.openai = openai;
        }

        public static class OpenAi {
            private String baseUrl = "https://api.openai.com";
            private String apiKey = "";
            private String model = "gpt-4o-mini";
            private int timeoutSeconds = 30;

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
            }

            public int getTimeoutSeconds() {
                return timeoutSeconds;
            }

            public void setTimeoutSeconds(int timeoutSeconds) {
                this.timeoutSeconds = timeoutSeconds;
            }
        }
    }

    public static class Orchestration {
        private boolean parallel = true;
        private int defaultMaxAttempts = 2;

        public boolean isParallel() {
            return parallel;
        }

        public void setParallel(boolean parallel) {
            this.parallel = parallel;
        }

        public int getDefaultMaxAttempts() {
            return defaultMaxAttempts;
        }

        public void setDefaultMaxAttempts(int defaultMaxAttempts) {
            this.defaultMaxAttempts = defaultMaxAttempts;
        }
    }
}
