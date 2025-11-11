package com.dearwith.dearwith_backend.common.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class HibernateQueryCountFilter implements Filter {

    private final Statistics stats;

    public HibernateQueryCountFilter(EntityManagerFactory emf) {
        var sf = emf.unwrap(org.hibernate.SessionFactory.class);
        this.stats = sf.getStatistics();
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        long before = stats.getQueryExecutionCount();
        long selectBefore = stats.getPrepareStatementCount();
        try {
            chain.doFilter(req, res);
        } finally {
            long after = stats.getQueryExecutionCount();
            long executed = after - before;
            System.out.println("[hibernate] queries in this request = " + executed);
        }
    }
}
