package test;

import org.jsoup.nodes.Element;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PomCodeRenderer {

    private static final List<String> CLICKABLE_INPUT_TYPES = List.of("button", "submit", "reset", "checkbox", "radio");

    String render(String className, List<GeneratedElement> generatedElements) {
        StringBuilder sb = new StringBuilder();
        Set<String> emittedFields = new LinkedHashSet<>();
        Set<String> emittedMethods = new LinkedHashSet<>();

        sb.append("import org.openqa.selenium.WebDriver;\n");
        sb.append("import org.openqa.selenium.WebElement;\n");
        sb.append("import org.openqa.selenium.support.FindBy;\n\n");
        sb.append("public class ").append(className).append(" {\n\n");
        sb.append("    private final WebDriver driver;\n\n");
        sb.append("    public ").append(className).append("(WebDriver driver) {\n");
        sb.append("        this.driver = driver;\n");
        sb.append("    }\n\n");

        for (GeneratedElement generated : generatedElements) {
            appendField(sb, generated, emittedFields);
        }

        for (GeneratedElement generated : generatedElements) {
            appendMethod(sb, generated.element, generated.fieldName, emittedMethods);
        }

        sb.append("}");
        return sb.toString();
    }

    private void appendField(StringBuilder sb, GeneratedElement generated, Set<String> emittedFields) {
        String fieldBlock = "    @FindBy(" +
                generated.primaryLocator.strategy +
                " = \"" +
                escapeJava(generated.primaryLocator.value) +
                "\")\n" +
                "    public WebElement " +
                generated.fieldName +
                ";\n\n";
        if (emittedFields.add(fieldBlock)) {
            sb.append(fieldBlock);
        }
    }

    private void appendMethod(StringBuilder sb, Element element, String fieldName, Set<String> emittedMethods) {
        String tagName = element.tagName();
        String inputType = element.attr("type").toLowerCase(Locale.ROOT);

        if ("input".equals(tagName) && CLICKABLE_INPUT_TYPES.contains(inputType)) {
            appendClickMethod(sb, fieldName, emittedMethods);
            return;
        }

        if ("input".equals(tagName) || "textarea".equals(tagName)) {
            appendEnterMethod(sb, fieldName, emittedMethods);
            return;
        }

        if ("button".equals(tagName) || "a".equals(tagName)) {
            appendClickMethod(sb, fieldName, emittedMethods);
        }
    }

    private void appendEnterMethod(StringBuilder sb, String fieldName, Set<String> emittedMethods) {
        String methodBlock = "    public void enter" + capitalize(fieldName) + "(String value) {\n" +
                "        " + fieldName + ".clear();\n" +
                "        " + fieldName + ".sendKeys(value);\n" +
                "    }\n\n";
        if (emittedMethods.add(methodBlock)) {
            sb.append(methodBlock);
        }
    }

    private void appendClickMethod(StringBuilder sb, String fieldName, Set<String> emittedMethods) {
        String methodBlock = "    public void click" + capitalize(fieldName) + "() {\n" +
                "        " + fieldName + ".click();\n" +
                "    }\n\n";
        if (emittedMethods.add(methodBlock)) {
            sb.append(methodBlock);
        }
    }

    private String escapeJava(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }
}
