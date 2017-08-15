package net.gddata.index.ui;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by zhangzf on 17/6/26.
 */
@RestController
@RequestMapping(value = "/fastdb")
public class FastDbWeb {


    @RequestMapping(value = "testUploadFiles", method = RequestMethod.POST)
    public Map<String,String> upload(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("textml;charset=UTF-8");

        try {
            request.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Part part = null;// myFileName是文件域的name属性值
        try {
            part = request.getPart("myFileName");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }
        // 文件类型限制
//        String[] allowedType = { "image/bmp", "image/gif", "image/jpeg", "image/png" };
//        boolean allowed = Arrays.asList(allowedType).contains(part.getContentType());
//        if (!allowed) {
//            try {
//                response.getWriter().write("error|不支持的类型");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        // 图片大小限制
        if (part.getSize() > 5 * 1024 * 1024) {
            try {
                response.getWriter().write("error|图片大小不能超过5M");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 包含原始文件名的字符串
        String fi = part.getHeader("content-disposition");
        // 提取文件拓展名
        String fileNameExtension = fi.substring(fi.indexOf("."), fi.length() - 1);
        // 生成实际存储的真实文件名
        String realName = UUID.randomUUID().toString() + fileNameExtension;
        // 图片存放的真实路径
//        String realPath = getServletContext().getRealPath("/files") + "/" + realName;
        // 将文件写入指定路径下
        try {
            part.write("/data/filecenter/"+realName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String,String> map = new HashMap();
        map.put("url","http://file.xylsok.com/"+realName);
        return map;

    }
}
