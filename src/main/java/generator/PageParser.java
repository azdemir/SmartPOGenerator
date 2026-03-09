package generator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class PageParser {

    public List<Element> parse(String url) throws Exception {

        Document doc = Jsoup.connect(url).get();

        Elements elements = doc.select(
                "input,button,select,textarea,a"
        );

        return new ArrayList<>(elements);
    }
}