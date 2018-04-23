package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.config.URIParam;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.ResourceFactory;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.ShibbolethUser;
import de.ids_mannheim.korap.user.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

/**
 * @author hanl
 * @date 14/01/2014
 */
public class RowMapperFactory {

    public static class UserMapMapper implements RowMapper<Map> {

        @Override
        public Map mapRow (ResultSet rs, int rowNum) throws SQLException {
            User user = new UserMapper().mapRow(rs, rowNum);
            return user.toMap();
        }
    }

    public static class UserMapper implements RowMapper<User> {

        @Override
        public User mapRow (ResultSet rs, int rowNum) throws SQLException {
            User user;
            switch (rs.getInt("type")) {
                case 0:
                    user = getKorAP(rs);
                    break;
//                case 1:
//                    user = getShib(rs);
//                    break;
                default:
                    user = User.UserFactory.getDemoUser();
                    user.setId(rs.getInt("id"));
                    user.setAccountCreation(rs.getTimestamp(
                            Attributes.ACCOUNT_CREATION).getTime());
                    return user;
            }
            return user;
        }


        private KorAPUser getKorAP (ResultSet rs) throws SQLException {
            KorAPUser user = User.UserFactory.getUser(rs
                    .getString(Attributes.USERNAME));
            user.setPassword(rs.getString(Attributes.PASSWORD));
            user.setId(rs.getInt(Attributes.ID));
            user.setAccountLocked(rs.getBoolean(Attributes.ACCOUNTLOCK));
            user.setAccountCreation(rs.getLong(Attributes.ACCOUNT_CREATION));
            user.setAccountLink(rs.getString(Attributes.ACCOUNTLINK));
            long l = rs.getLong(Attributes.URI_EXPIRATION);

            URIParam param = new URIParam(
                    rs.getString(Attributes.URI_FRAGMENT), l == 0 ? -1
                            : new Timestamp(l).getTime());
            user.addField(param);
            return user;
        }


//        private ShibbolethUser getShib (ResultSet rs) throws SQLException {
//            ShibbolethUser user = User.UserFactory.getShibInstance(
//                    rs.getString(Attributes.USERNAME),
//                    rs.getString(Attributes.MAIL), rs.getString(Attributes.CN));
//            user.setId(rs.getInt(Attributes.ID));
//            return user;
//        }

    }

    public static class AuditMapper implements RowMapper<AuditRecord> {

        @Override
        public AuditRecord mapRow (ResultSet rs, int rowNum)
                throws SQLException {
            AuditRecord r = new AuditRecord(AuditRecord.CATEGORY.valueOf(rs
                    .getString("aud_category")));
            r.setUserid(rs.getString("aud_user"));
            r.setField_1(rs.getString("aud_field_1"));
            r.setTimestamp(rs.getTimestamp("aud_timestamp").getTime());
            r.setId(rs.getInt("aud_id"));
            r.setStatus(rs.getInt("aud_status"));
            r.setLoc(rs.getString("aud_location"));
            return r;
        }
    }

    public static class ResourceMapper implements RowMapper<KustvaktResource> {

        @Override
        public KustvaktResource mapRow (ResultSet rs, int rowNum)
                throws SQLException {
            KustvaktResource r = ResourceFactory.getResource(rs.getInt("type"));
            if (r != null) {
                r.setId(rs.getInt("id"));
                r.setName(rs.getString("name"));

                r.setFields(rs.getString("data"));
                r.setDescription(rs.getString("description"));
                r.setCreated(rs.getLong("created"));
                r.setPath(rs.getString("name_path"));
                r.setPersistentID(rs.getString("persistent_id"));
            }
            return r;
        }

    }
}
