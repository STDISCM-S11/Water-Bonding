import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {
    
    private int id;
    private String action;
    private String formattedTimestamp;
    private String element;

    // Define the desired date-time format
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Log(int id, String action, LocalDateTime timestamp){
        this.id = id;
        this.action = action;
        this.formattedTimestamp = timestamp.format(formatter);
    }

    public Log(int id, String action, LocalDateTime timestamp, String element){
        this.id = id;
        this.action = action;
        this.formattedTimestamp = timestamp.format(formatter);
        this.element = element;
    }

    @Override
    public String toString(){
        // Use the formatter to format the timestamp
        return "(" + id + ", " + action + ", " + formattedTimestamp + ")";
    }

    public String toStringElement(){
        // Use the formatter to format the timestamp
        // return "(" + id + ", " + element + ", " + action + ", " + formattedTimestamp + ")";
        return "("+ element+ id + ", "+ action+ ", " + formattedTimestamp+ ")";
    }

}
