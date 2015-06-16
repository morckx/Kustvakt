package de.ids_mannheim.korap.interfaces;

import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.user.User;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

/**
 * User: hanl
 * Date: 8/20/13
 * Time: 10:45 AM
 */
//fixme: move table to different database!
public abstract class AuditingIface implements Runnable {

    protected static int BATCH_SIZE = 15;
    protected final List<AuditRecord> records = new ArrayList<>(BATCH_SIZE + 5);
    private final List<AuditRecord> buffer = new ArrayList<>(BATCH_SIZE + 5);

    public abstract <T extends AuditRecord> List<T> retrieveRecords(
            AuditRecord.CATEGORY category, DateTime day, DateTime until,
            boolean exact, int limit);

    public abstract <T extends AuditRecord> List<T> retrieveRecords(
            AuditRecord.CATEGORY category, User user, int limit);

    public abstract <T extends AuditRecord> List<T> retrieveRecords(
            LocalDate day, int hitMax);

    public abstract <T extends AuditRecord> List<T> retrieveRecords(
            String userID, LocalDate start, LocalDate end, int hitMax);

    private void addAndRun(AuditRecord record) {
        if (buffer.size() > BATCH_SIZE) {
            records.clear();
            records.addAll(buffer);
            run();
            buffer.clear();
        }
        if (buffer.size() <= BATCH_SIZE)
            buffer.add(record);
    }

    public <T extends AuditRecord> void audit(T request) {
        addAndRun(request);
    }

    public <T extends AuditRecord> void audit(List<T> requests) {
        for (T rec : requests)
            addAndRun(rec);
    }

}
