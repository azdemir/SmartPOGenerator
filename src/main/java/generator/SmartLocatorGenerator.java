package generator;

import org.jsoup.nodes.Element;

public class SmartLocatorGenerator {

    public static LocatorResult generate(Element el){

        if(!el.id().isEmpty())
            return new LocatorResult("id", el.id(),100);

        if(!el.attr("data-testid").isEmpty())
            return new LocatorResult("css","[data-testid='"+el.attr("data-testid")+"']",95);

        if(!el.attr("name").isEmpty())
            return new LocatorResult("name", el.attr("name"),90);

        if(!el.attr("aria-label").isEmpty())
            return new LocatorResult("css","[aria-label='"+el.attr("aria-label")+"']",85);

        if(!el.className().isEmpty())
            return new LocatorResult("css","."+el.className().replace(" ","."),70);

        if(!el.attr("placeholder").isEmpty())
            return new LocatorResult("css","[placeholder='"+el.attr("placeholder")+"']",60);

        if(!el.text().isEmpty() && el.text().length()<20)
            return new LocatorResult("xpath","//"+el.tagName()+"[contains(text(),'"+el.text()+"')]",50);

        return new LocatorResult("xpath","//"+el.tagName(),30);
    }

}
