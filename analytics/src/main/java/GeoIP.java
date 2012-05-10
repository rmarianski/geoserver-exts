import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;


public class GeoIP {
    
    /**
     * cached geoip lookup service
     */
    static LookupService geoIPLookup;
    
    public static void main(String[] args) throws Exception {
        String[] ips = new String[]{
            "173.236.65.91", "199.103.56.11", "212.149.159.110","203.11.83.3",
            "150.229.12.128", "74.125.127.103", "142.32.252.190", "205.166.175.88",
            "62.149.128.163", "208.98.229.40", "170.28.8.40", "200.1.116.61", "196.211.204.218",
            
        };
        
        GeoLocation [] locs = new GeoLocation[ips.length];
        
        LookupService lookup = lookupGeoIPDatabase();
        for (int i = 0; i < ips.length; i++) {
            String ip = ips[i];
            
            Location loc = lookup.getLocation(ip);
            InetAddress addr = InetAddress.getByName(ip);
            
            GeoLocation gloc = new GeoLocation();
            gloc.country = loc.countryName;
            gloc.city = loc.city;
            gloc.host = addr.getHostName();
            gloc.ip = ip;
            gloc.lon = loc.longitude;
            gloc.lat = loc.latitude;
            locs[i] = gloc;
        }
        
        //Class.forName("org.postgresql.Driver");
        Class.forName("org.h2.Driver");
        Connection cx = 
            DriverManager.getConnection("jdbc:h2:/Users/jdeolive/scratch/data_dir/monitoring/monitoring");
        //Connection cx = 
        //    DriverManager.getConnection("jdbc:postgresql://localhost:5432/monitoring", "jdeolive", "");
        Statement st = cx.createStatement();
        ResultSet rs = st.executeQuery("SELECT id FROM request");
        
        PreparedStatement ps = cx.prepareStatement("UPDATE request SET remote_country = ?, " + 
            "remote_city = ?, remote_lon = ?, remote_lat = ?, remote_address = ?, remote_host = ? " + 
            " WHERE id = ?");
        
        Random r = new Random();
        while(rs.next()) {
            int i = r.nextInt(locs.length);
            GeoLocation loc = locs[i];
            
            ps.setString(1, loc.country);
            ps.setString(2, loc.city);
            ps.setDouble(3, loc.lon);
            ps.setDouble(4, loc.lat);
            ps.setString(5, loc.ip);
            ps.setString(6, loc.host);
            ps.setInt(7, rs.getInt(1));
            
            ps.execute();
        }
        
        ps.close();
        rs.close();
        st.close();
        cx.close();
    }

    static LookupService lookupGeoIPDatabase() {
        try {
            File f = new File("/Users/jdeolive/Downloads/GeoLiteCity.dat");
            return new LookupService(f);
        } 
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    static class GeoLocation {
        String country, city, host, ip;
        double lon, lat;
        
       
    }
}
