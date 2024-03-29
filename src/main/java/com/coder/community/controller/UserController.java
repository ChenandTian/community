package com.coder.community.controller;

import com.coder.community.annotation.LoginRequired;
import com.coder.community.entity.User;
import com.coder.community.service.FollowService;
import com.coder.community.service.LikeService;
import com.coder.community.service.UserService;
import com.coder.community.util.CommunityConstant;
import com.coder.community.util.CommunityUtil;
import com.coder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketKey;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model){
        // 生成上传文件名称
        String fileName = CommunityUtil.generateUUID();
        // 设置响应信息
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));

        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketKey, fileName, 3600,policy);

        model.addAttribute("uploadToken",uploadToken);
        model.addAttribute("fileName", fileName);

        return "/site/setting";
    }

    //更新头像的路径
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if(StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1,"文件名不能为空！");
        }

        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);

        return CommunityUtil.getJSONString(0);
    }


    // 废弃
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "你还没有选择图片!");
            return "/site/setting";
        }

        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件格式不正确!");
            return "/site/setting";
        }
        //生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;

        //确定文件存放位置
        File dest = new File(uploadPath + "/" + fileName);

        //将headerImage中的文件写入文件
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败！" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！", e);
        }

        //更新当前用户的头像路径(web的访问路径)
        //http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);
        //这里使用请求重定向不是使用请求转

        return "redirect:/index";
    }
    @RequestMapping(path = "/update", method = RequestMethod.POST)
    public String updatePassword(String oldPassword,String newPassword,String confirmPassword, Model model) {
//        System.out.println("我在Controller中" + oldPassword + newPassword);

        User user = hostHolder.getUser();
        user = userService.findUserById(user.getId());
        System.out.println(user);
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        System.out.println(oldPassword);
        if(!user.getPassword().equals(oldPassword)){
            model.addAttribute("passwordError", "原密码输入错误！");
            System.out.println("我又错了");
            return "/site/setting";
        }

        if(newPassword != confirmPassword) {
            model.addAttribute("ReError", "两次输入的密码不一致!");
            return "/site/setting";
        }
       user.setPassword(CommunityUtil.md5(newPassword + user.getSalt()));

       userService.updatePassword(user.getId(), user.getPassword());

       return "redirect:/index";
    }


    // 废弃
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        //服务器存放路径
        fileName = uploadPath + "/" +fileName;
        //向浏览器输出图片（文件后缀）
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        //响应图片
        response.setContentType("image/" + suffix);
        try (
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
                ){

            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1){
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败：" + e.getMessage());
        }
    }

    //个人主页
    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user = userService.findUserById(userId);
        if(user == null){
            throw new RuntimeException("该用户不存在!");
        }

        //用户
        model.addAttribute("user",user);
        //点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        //关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER ,userId);
        model.addAttribute("followerCount",followerCount);
        //当前登录账号对这个账户是否已经关注
        boolean hasFollowed = false;
        if(hostHolder.getUser() != null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }

        model.addAttribute("hasFollowed",hasFollowed);

        return "/site/profile";
    }


}
