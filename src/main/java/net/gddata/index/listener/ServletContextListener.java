package net.gddata.index.listener;

import net.gddata.index.utils.IndexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;

/**
 * Created by zhangzf on 16/9/18.
 */
@Component("servletContextListener")
public class ServletContextListener implements javax.servlet.ServletContextListener {

    @Autowired
    IndexUtils indexUtils;
    @Value("${HDBSMIndex.rootPath}")
    private String rootPath;

    @Value("${properties.name}")
    private String propertiesName;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("使用：" + propertiesName);
        String indexId = indexUtils.checkOrInitIndex(null, null);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
