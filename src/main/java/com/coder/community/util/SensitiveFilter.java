package com.coder.community.util;

import com.sun.org.apache.regexp.internal.RE;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 定义前缀树
 */
@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    //定义一个常量(替换符)
    private static final String REPLACEMENT = "***";

    //初始化根节点
    private TrieNode rootNode = new TrieNode();

    //根据txt文件(@PostConstruct初始化方法，在容器被调用后会自动调用)
    @PostConstruct
    public void init(){
        try(
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                ){
            String keyword;
            while ((keyword = reader.readLine()) != null ){
                //添加到前缀树
                this.addKeyword(keyword);
            }
        }catch (IOException e){
            logger.error("加载敏感词文件失败:" + e.getMessage());
        }
    }

    //讲一个敏感词添加到前缀树中去
    private void addKeyword(String keyword){
        TrieNode tempNode = rootNode;
        for(int i = 0; i < keyword.length() ; i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);

            if(subNode == null){
                //初始化子节点
                subNode = new TrieNode();
                tempNode.addSunNode(c, subNode);
            }
            //让指针指向子节点，进入下一轮循环
            tempNode = subNode;

            //设置结束标示
            if(i == keyword.length()-1){
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     *
     * @param text
     * @return
     */
    public String filter(String text){
        if(StringUtils.isBlank(text)){
            return null;

        }

        //指针1
        TrieNode tempNode = rootNode;
        //指针2
        int begin = 0;
        //指针3
        int position = 0;
        //结果
        StringBuilder sb = new StringBuilder();

        while (position < text.length()){
            char c = text.charAt(position);

            //跳过符号
            if(isSymbol(c)){
                //若指针1处于根节点,将此符号计入结果，让指针2向下走一步
                if(tempNode == rootNode){
                    sb.append(c);
                    begin++;
                }
                //无论符号在开头或中间，指针3都向下走一步
                position++;

                continue;
            }

            // 检查下级节点
            tempNode = tempNode.getSubNode(c);
            if(tempNode == null){
                // 以begin为开头的字符串不是敏感词
                sb.append(text.charAt(begin));

                //进入下一个位置
                position = ++begin;
                //重新指向根节点
                tempNode = rootNode;

            }else if(tempNode.isKeywordEnd()){
                //发现敏感词，将begin-position字符串替换掉
                sb.append(REPLACEMENT);
                //进入下一个位置
                begin =  ++position;
                //重新指向根节点
                tempNode = rootNode;

            }else {
                //检查下一个字符
                position++;
            }
        }

        //将最后一批字符计入结果
        sb.append(text.substring(begin));

        return sb.toString();
    }

    //判断是否为符号
    private boolean isSymbol(Character c) {
        //(c < 0x2E80 || c > 0x9FFF)是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }
    //前缀树
    private class TrieNode{
        //描述关键词结束标示
        private boolean isKeywordEnd = false;

        //描述当前节点的子节点(key是下级字符，value是下级节点)
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        // 添加子节点，
        public void addSunNode(Character c , TrieNode node){
            subNodes.put(c, node);
        }

        //获取子节点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }
    }
}
