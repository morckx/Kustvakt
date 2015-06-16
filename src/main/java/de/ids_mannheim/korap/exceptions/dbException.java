package de.ids_mannheim.korap.exceptions;

import de.ids_mannheim.korap.auditing.AuditRecord;

import java.util.Arrays;

/**
 * @author hanl
 * @date 08/04/2015
 */
public class dbException extends KorAPException {

    private dbException(Object userid, Integer status, String message,
            String args) {
        super(String.valueOf(userid), status, message, args);
    }

    public dbException(Object userid, String target, Integer status,
            String... args) {
        this(userid, status, "",
                Arrays.asList(args).toString());
        AuditRecord record = new AuditRecord(AuditRecord.CATEGORY.DATABASE);
        record.setUserid(String.valueOf(userid));
        record.setStatus(status);
        record.setTarget(target);
        record.setArgs(this.getEntity());
        this.records.add(record);
    }

    public dbException(KorAPException e, Integer status, String... args) {
        this(e.getUserid(), e.getStatusCode(), e.getMessage(), e.getEntity());
        AuditRecord record = AuditRecord
                .dbRecord(e.getUserid(), status, args);
        record.setField_1(e.toString());
        this.records.addAll(e.getRecords());
        this.records.add(record);
    }

    @Override
    public String toString() {
        return "DBExcpt{" +
                "status=" + getStatusCode() +
                ", message=" + getMessage() +
                ", args=" + getEntity() +
                ", userid=" + this.getUserid() +
                '}';
    }
}
