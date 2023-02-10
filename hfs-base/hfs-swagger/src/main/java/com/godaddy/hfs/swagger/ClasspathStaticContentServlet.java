package com.godaddy.hfs.swagger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClasspathStaticContentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(ClasspathStaticContentServlet.class);

    final List<String> pkgs;

    final List<String> indexFiles;

    final ClassLoader classLoader;

    public ClasspathStaticContentServlet(List<String> pkgs) {
        this(null, pkgs, Arrays.asList("index.html"));
    }

    public ClasspathStaticContentServlet(ClassLoader classLoader, List<String> pkgs, List<String> indexFiles) {

        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }

        // ensure all packages end with slash
        for (int i=0; i<pkgs.size(); i++) {
            String pkg = pkgs.get(i);
            if (pkg.endsWith("/")) {
                throw new IllegalArgumentException("package cannot end with slash: " + pkg);
            }
        }

        this.pkgs = pkgs;
        this.indexFiles = indexFiles;
        this.classLoader = classLoader;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        for (String pkg : pkgs) {

            String path = pkg + req.getPathInfo();

            if (path.endsWith("/")) {

                // check index files
                for (String indexFile : indexFiles) {
                    String indexPath = path + indexFile;
                    logger.trace("index path: {}", indexPath);

                    InputStream is = classLoader.getResourceAsStream(indexPath);
                    if (is != null) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        OutputStream os = resp.getOutputStream();
                        IOUtils.copy(is, os);
                        return;
                    }
                }

            } else {

                logger.trace("path: {}", path);

                InputStream is = classLoader.getResourceAsStream(path);
                if (is != null) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    OutputStream os = resp.getOutputStream();
                    IOUtils.copy(is, os);
                    return;
                }
            }
        }

        // if we've reached here, we couldn't find the resource in any of the packages
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

}
