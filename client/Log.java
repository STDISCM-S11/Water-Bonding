import java.time.LocalDateTime;

public class Log {
    
    private int id;
    private String action;
    private LocalDateTime timestamp;
    private String element;

    public Log(int id, String action, LocalDateTime timestamp){
        this.id = id;
        this.action = action;
        this.timestamp = timestamp;
    }

    public Log(int id, String action, LocalDateTime timestamp, String element){
        this.id = id;
        this.action = action;
        this.timestamp = timestamp;
        this.element = element;
    }

    @Override
    public String toString(){
        return "(" + id + ", " + action + ", " + timestamp + ")";
    }

    public String toStringElement(){
        return "(" + id + ", " + element + ", " + action + ", " + timestamp + ")";
    }

}
