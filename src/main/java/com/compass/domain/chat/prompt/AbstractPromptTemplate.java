package com.compass.domain.chat.prompt;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for prompt templates with common functionality
 */
public abstract class AbstractPromptTemplate implements PromptTemplate {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    protected final String name;
    protected final String description;
    protected final String template;
    protected final String[] requiredParameters;
    protected final String[] optionalParameters;
    
    protected AbstractPromptTemplate(String name, String description, String template,
                                    String[] requiredParameters, String[] optionalParameters) {
        this.name = name;
        this.description = description;
        this.template = template;
        this.requiredParameters = requiredParameters != null ? requiredParameters : new String[0];
        this.optionalParameters = optionalParameters != null ? optionalParameters : new String[0];
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getTemplate() {
        return template;
    }
    
    @Override
    public String buildPrompt(Map<String, Object> parameters) {
        if (!validateParameters(parameters)) {
            // Log which parameters are missing for debugging
            StringBuilder missingParams = new StringBuilder();
            for (String required : requiredParameters) {
                if (parameters == null || !parameters.containsKey(required) || parameters.get(required) == null) {
                    if (missingParams.length() > 0) missingParams.append(", ");
                    missingParams.append(required);
                }
            }
            throw new IllegalArgumentException("Missing required parameters for template '" + name + "': " + missingParams.toString());
        }
        
        String result = template;
        
        // Replace all placeholders with actual values
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String paramName = matcher.group(1).trim();
            Object value = parameters.get(paramName);
            
            if (value != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value.toString()));
            } else if (Arrays.asList(optionalParameters).contains(paramName)) {
                // Optional parameter not provided, remove placeholder
                matcher.appendReplacement(sb, "");
            } else {
                // Keep placeholder if no value provided (shouldn't happen after validation)
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString().trim();
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return requiredParameters.length == 0;
        }
        
        for (String required : requiredParameters) {
            if (!parameters.containsKey(required) || parameters.get(required) == null) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String[] getRequiredParameters() {
        return Arrays.copyOf(requiredParameters, requiredParameters.length);
    }
    
    @Override
    public String[] getOptionalParameters() {
        return Arrays.copyOf(optionalParameters, optionalParameters.length);
    }
}