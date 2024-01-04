package shx.kitsune;

import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.configuration.file.FileConfiguration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {
    private HikariDataSource dataSource;

    public Database() {
        FileConfiguration config = Kitsune.getPlugin().getConfig();
        if ( config.getString("db.host").isBlank() ) {
            Kitsune.getPlugin().getLogger().warning("Database host is not set in config.yml");
            return;
        }

        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setUsername(config.getString("db.username"));
        hikariConfig.setPassword(config.getString("db.password"));
        hikariConfig.setMaximumPoolSize(config.getInt("db.max_pool_size"));
        hikariConfig.setConnectionTimeout(config.getInt("db.connection_timeout"));
        hikariConfig.setIdleTimeout(config.getInt("db.idle_timeout"));
        hikariConfig.setLeakDetectionThreshold(config.getInt("db.leak_detection_threshold"));
        hikariConfig.setJdbcUrl(getJDBCUrl(
            config.getString("db.host"),
            config.getInt("db.port"),
            config.getString("db.database"))
        );

        dataSource = new HikariDataSource(hikariConfig);
    }

    private String getJDBCUrl(String host, int port, String database) {
        return "jdbc:mysql://" + host + ":" + port + "/" + database;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if ( dataSource != null ) {
            dataSource.close();
        }
    }
}
