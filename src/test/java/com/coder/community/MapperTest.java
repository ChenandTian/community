package com.coder.community;

import com.coder.community.dao.*;
import com.coder.community.entity.*;
import com.coder.community.service.MessageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Test
    public void testSelectUser(){
        User user = userMapper.selectById(101);
        System.out.println(user);

        user = userMapper.selectByName("liubei");
        System.out.println(user);

        user = userMapper.selectByEmail("nowcoder101@sina.com");
        System.out.println(user);
    }

    @Test
    public void testInsertUser(){
        User user = new User();
        user.setUsername("test");
        user.setPassword("123456");
        user.setEmail("32222222222@qq.com");
        user.setSalt("abc");
        user.setHeaderUrl("http://www.nowcoder.com/101.png");
        user.setCreateTime(new Date());

        int rows = userMapper.insertUser(user);
        System.out.println(rows);
        System.out.println(user.getId());
    }

    @Test
    public void testUpdate(){
        User user = new User();
        int rows = userMapper.updateHeader(156,"http://www.nowcoder.com/103.png");
        System.out.println(rows);

        int row = userMapper.updatePassword(156,"qwe123");
        System.out.println(row);

        int r = userMapper.updateStatus(156,1);
        System.out.println(r);
    }

    @Test
    public void selectPosts(){
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(149,0,10,0);
        for(DiscussPost post : list){
            System.out.println(post);
        }

        int rows = discussPostMapper.selectDiscussPostRows(149);
        System.out.println(rows);
    }

    @Test
    public void testInsertLoginTicket(){
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(101);
        loginTicket.setTicket("abc");
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000 * 60 * 10));

        loginTicketMapper.insertLoginTicket(loginTicket);
    }

    @Test
    public void testSelectLoginTicket(){
        LoginTicket loginTicket = new LoginTicket();
        loginTicket = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket);

        loginTicketMapper.updateStatus("abc",1);
        loginTicket = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket);
    }

    @Test
    public void testSelectComment(){
        List<Comment> comments = new ArrayList<>();
        comments = commentMapper.selectCommentByEntity(1,270,0,6);
        System.out.println(comments);

        int a = commentMapper.selectCountByEntity(2,12);
        System.out.println(a);
    }

    @Test
    public void testSelectMessage(){
        List<Message> list = messageMapper.selectConversations(111,0,10);
        System.out.println(list);

        int a = messageMapper.selectConversationCount(111);
        System.out.println(a);

        list = messageMapper.selectLetters("111_112", 0, 10);
        System.out.println(list);

        a = messageMapper.selectLetterCount("111_112");
        System.out.println(a);

        a = messageMapper.selectLetterUnreadCount(111,null);
        System.out.println(a);
    }


    @Test
    public void testUpdatePassword() {
        int rows = userMapper.updatePassword(112,"ccc");
        System.out.println(rows);
    }
}
