package de.ids_mannheim.korap.handlers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.URIParam;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 14/01/2014
 */
public class RowMapperFactory {

    public static class UserMapMapper implements RowMapper<Map<?,?>> {

        @Override
        public Map<?, ?> mapRow (ResultSet rs, int rowNum) throws SQLException {
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

}
