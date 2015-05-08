package net.microtrash.wisperingtree.bus;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by steph on 5/1/15.
 */
@Table(name = "LogMessage")
public class LogMessage extends Model {

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("H:m:s", Locale.ENGLISH);

    @Column(name = "Text")
    private String mText;

    @Column(name = "CreatedOn")
    private Date mDate;

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public LogMessage() {
    }

    public LogMessage(String text) {
        mText = text;
        mDate = Calendar.getInstance().getTime();
    }

    public Date getDate() {
        return mDate;
    }

    public String toString() {
        if(getDate() == null){
            return getText();
        }
        return dateFormat.format(getDate()) + ": " + getText();
    }
}
