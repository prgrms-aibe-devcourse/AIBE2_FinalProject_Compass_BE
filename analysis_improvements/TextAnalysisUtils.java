package com.compass.domain.media.util;

/**
 * Utility class for text analysis operations used in OCR processing
 */
public final class TextAnalysisUtils {
    
    private TextAnalysisUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Counts the number of words in the given text.
     * Handles null and empty strings gracefully.
     * 
     * @param text the text to analyze
     * @return the number of words
     */
    public static int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        
        // Split by whitespace, tabs, and newlines
        String[] words = text.trim().split("\\s+");
        return words.length;
    }
    
    /**
     * Counts the number of lines in the given text.
     * Handles null and empty strings gracefully.
     * 
     * @param text the text to analyze
     * @return the number of lines
     */
    public static int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // Split by various line break patterns
        String[] lines = text.split("\r\n|\r|\n");
        return lines.length;
    }
    
    /**
     * Calculates basic text statistics.
     * 
     * @param text the text to analyze
     * @return TextStats object containing word count, line count, and character count
     */
    public static TextStats analyzeText(String text) {
        return TextStats.builder()
                .wordCount(countWords(text))
                .lineCount(countLines(text))
                .characterCount(text != null ? text.length() : 0)
                .build();
    }
    
    /**
     * Text statistics container
     */
    public static class TextStats {
        private final int wordCount;
        private final int lineCount;
        private final int characterCount;
        
        private TextStats(int wordCount, int lineCount, int characterCount) {
            this.wordCount = wordCount;
            this.lineCount = lineCount;
            this.characterCount = characterCount;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public int getWordCount() { return wordCount; }
        public int getLineCount() { return lineCount; }
        public int getCharacterCount() { return characterCount; }
        
        public static class Builder {
            private int wordCount;
            private int lineCount;
            private int characterCount;
            
            public Builder wordCount(int wordCount) {
                this.wordCount = wordCount;
                return this;
            }
            
            public Builder lineCount(int lineCount) {
                this.lineCount = lineCount;
                return this;
            }
            
            public Builder characterCount(int characterCount) {
                this.characterCount = characterCount;
                return this;
            }
            
            public TextStats build() {
                return new TextStats(wordCount, lineCount, characterCount);
            }
        }
    }
}