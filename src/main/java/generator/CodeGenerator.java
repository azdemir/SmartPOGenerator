package generator;

import org.jsoup.nodes.Element;

import java.util.List;

public class CodeGenerator {

    public String generate(String className, List<Element> elements) {

        StringBuilder code = new StringBuilder();
   

        code.append("import org.openqa.selenium.*;\n");
        code.append("import org.openqa.selenium.support.*;\n\n");

        code.append("public class ").append(className).append(" {\n\n");

        code.append("WebDriver driver;\n\n");

        code.append("public ").append(className).append("(WebDriver driver){\n");
        code.append("this.driver = driver;\n");
        code.append("PageFactory.initElements(driver,this);\n");
        code.append("}\n\n");

        for(Element el : elements){

//            String locator = LocatorBuilder.buildLocator(el);
            LocatorResult locator = SmartLocatorGenerator.generate(el);

            code.append("@FindBy(")
            .append(locator.strategy)
            .append("=\"")
            .append(locator.value)
            .append("\")\n");
            String name = generateName(el);

            code.append(locator).append("\n");
            code.append("WebElement ").append(name).append(";\n\n");

            generateMethod(code, el, name);
        }

        code.append("}");

        return code.toString();
    }

    private String generateName(Element el){

        if(!el.id().isEmpty())
            return el.id();

        if(!el.attr("name").isEmpty())
            return el.attr("name");

        return el.tagName() + el.elementSiblingIndex();
    }

    private void generateMethod(StringBuilder code, Element el, String name){

        if(el.tagName().equals("input")){

            code.append("public void enter")
                    .append(cap(name))
                    .append("(String value){\n");

            code.append(name).append(".sendKeys(value);\n");
            code.append("}\n\n");

        }

        if(el.tagName().equals("button")){

            code.append("public void click")
                    .append(cap(name))
                    .append("(){\n");

            code.append(name).append(".click();\n");
            code.append("}\n\n");

        }

    }

    private String cap(String str){
        return str.substring(0,1).toUpperCase()+str.substring(1);
    }

}