package generator;

import org.jsoup.nodes.Element;

public class LocatorBuilder {

    public static String buildLocator(Element el) {

        if(!el.id().isEmpty())
            return "@FindBy(id=\"" + el.id() + "\")";

        if(!el.attr("name").isEmpty())
            return "@FindBy(name=\"" + el.attr("name") + "\")";

        if(!el.className().isEmpty())
            return "@FindBy(css=\"." + el.className().replace(" ",".") + "\")";

        return "@FindBy(xpath=\"//" + el.tagName() + "\")";
    }

}