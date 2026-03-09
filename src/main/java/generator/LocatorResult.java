package generator;

public class LocatorResult {

    public String strategy;
    public String value;
    public int score;

    public LocatorResult(String strategy,String value,int score){
        this.strategy=strategy;
        this.value=value;
        this.score=score;
    }

}