package mvc.dao;

import mvc.common.DBManager;
import mvc.dto.TicketDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TicketDAOImpl implements TicketDAO {
    private static final TicketDAO instance = new TicketDAOImpl();

    /**
     * 외부에서 객체 생성 막음
     **/
    private TicketDAOImpl() {
    }

    public static TicketDAO getInstance() {
        return instance;
    }

    /**
     * 예매 등록
     **/
    @Override
    public int ticketInsert(TicketDTO ticket) {
        Connection con = null;
        PreparedStatement ps = null;
//        ResultSet rs = null;
        String sql = "insert into TICKET (TICKET_ID, USER_ID, SEATNUM, MUSICAL_ID, ISSUE) values (?,?,?,?,SYSDATE);";
        int result = 0;
        try {
            con = DBManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, ticket.getTicketId());
            ps.setString(2, ticket.getUserId());
            ps.setString(3, ticket.getSeatNum());
            ps.setInt(4, ticket.getMusicalId());
            result = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBManager.releaseConnection(con, ps);
        }
        return result;
    }

    /**
     * 예매 취소
     **/
    @Override
    public int ticketDelete(int ticketID) {
        Connection con = null;
        PreparedStatement ps = null;

        StringBuilder sql = new StringBuilder();
        sql.append("delete from ticket ");
        sql.append("where (ticket_id = ?) ");
        sql.append("and (TO_CHAR(issue, 'YYYY-MM-DD HH:MI:SS') >= TO_CHAR(sysdate - 1/24/3, 'YYYY-MM-DD HH:MI:SS'))"); // 뮤지컬 공연 시간 20 분 전까지만 취소 가능

        TicketDTO ticketDTO = null;
        int result = 0;

        try {
            ticketDTO = ticketSelectByTicketId(ticketID);
            String seatNum = ticketDTO.getSeatNum(); // 해당 티켓의 좌석 번호
            int musicalID = ticketDTO.getMusicalId(); // 해당 티켓(예매한 뮤지컬)의 뮤지컬 아이디

            con = DBManager.getConnection();
            ps = con.prepareStatement(sql.toString());
            ps.setInt(1, ticketID);

            cancelSeat(con, seatNum, musicalID); // 해당 티켓의 좌석 공석으로 전환

            result = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBManager.releaseConnection(con, ps);
        }

        return result;
    }

    /**
     * 개별 유저 예매 내역 조회
     **/
    @Override
    public TicketDTO ticketSelectByTicketId(int ticketID) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql = "select * from TICKET where TICKET_ID=?";
        TicketDTO result = null;
        try {
            con = DBManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, ticketID);
            rs = ps.executeQuery();
            if (rs.next()) {
                result = new TicketDTO(
                        rs.getInt("TICKET_ID"),
                        rs.getString("USER_ID"),
                        rs.getString("SEATNUM"),
                        rs.getInt("MUSICAL_ID"),
                        rs.getString("ISSUE")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBManager.releaseConnection(con, ps, rs);
        }
        return result;
    }

    @Override
    public List<TicketDTO> ticketSelectByUserId(String userId) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql = "select * from TICKET where USER_ID=?";
        List<TicketDTO> result = new ArrayList<>();
        try {
            con = DBManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                result.add(new TicketDTO(
                        rs.getInt("TICKET_ID"),
                        rs.getString("USER_ID"),
                        rs.getString("SEATNUM"),
                        rs.getInt("MUSICAL_ID"),
                        rs.getString("ISSUE")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBManager.releaseConnection(con, ps, rs);
        }
        return result;
    }

    /**
     * 티켓의 좌석 공석으로 전환
     **/
    private void cancelSeat(Connection con, String seatNum, int musicalID) throws SQLException {
        PreparedStatement ps = null;
        String sql = "update seat set sold = ? where (seatnum = ?) and (musical_id = ?)";

        try {
            ps = con.prepareStatement(sql);
            ps.setString(1, "N");
            ps.setString(2, seatNum);
            ps.setInt(3, musicalID);

            ps.executeUpdate();
        } finally {
            DBManager.releaseConnection(null, ps);
        }
    }
}