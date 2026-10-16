package com.ruoyi.system.service.mes.ai.impl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

/**
 * AI 问答知识库：classpath 下的系统使用文档，按 "## " 章节切分，
 * 对用户问题做 2-gram 关键词打分，取得分最高的若干章节注入 prompt。
 *
 * <p>语料小（几篇帮助文档），关键词检索足够，不引入向量服务。
 * 检索结果为空（得分全 0）说明问题大概率与系统使用无关，交由拒答护栏处理。
 *
 * @author qixiaoxia
 */
@Service
public class AiKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeService.class);
    private static final String KNOWLEDGE_PATH = "ai/mes-help-knowledge.md";
    /** 最多注入的章节数 */
    private static final int TOP_SECTIONS = 4;
    /** 注入 prompt 的知识总长度上限（控制 token 成本） */
    private static final int MAX_KNOWLEDGE_CHARS = 3000;
    /** 永远注入的索引章节标题（让模型知道系统有哪些功能） */
    private static final String INDEX_SECTION_TITLE = "系统功能清单";

    private final List<Section> sections = new ArrayList<>();

    @PostConstruct
    public void load() {
        try (InputStream in = new ClassPathResource(KNOWLEDGE_PATH).getInputStream()) {
            String md = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            StringBuilder body = new StringBuilder();
            String title = null;
            for (String line : md.split("\n")) {
                if (line.startsWith("## ")) {
                    flushSection(title, body);
                    title = line.substring(3).trim();
                    body.setLength(0);
                } else {
                    body.append(line).append('\n');
                }
            }
            flushSection(title, body);
            log.info("AI 知识库加载完成，共 {} 个章节", sections.size());
        } catch (Exception e) {
            log.error("AI 知识库加载失败：{}", KNOWLEDGE_PATH, e);
        }
    }

    private void flushSection(String title, StringBuilder body) {
        if (title != null && body.length() > 0) {
            sections.add(new Section(title, body.toString().trim()));
        }
    }

    /**
     * 检索与问题最相关的章节，拼成注入 prompt 的文本。
     *
     * @return 相关知识文本；问题与系统无关（全 0 分）时返回 null
     */
    public String retrieve(String question) {
        if (question == null || sections.isEmpty()) {
            return null;
        }
        List<String> ngrams = extractNgrams(question);
        List<Section> hits = new ArrayList<>();
        int totalScore = 0;
        for (Section s : sections) {
            int score = score(s, ngrams);
            if (score > 0) {
                hits.add(s);
                totalScore += score;
            }
            s.score = score;
        }
        if (totalScore == 0) {
            return null;
        }
        hits.sort((a, b) -> Integer.compare(b.score, a.score));
        StringBuilder sb = new StringBuilder();
        appendSection(sb, indexSection());
        for (Section s : hits.subList(0, Math.min(TOP_SECTIONS, hits.size()))) {
            appendSection(sb, s);
            if (sb.length() >= MAX_KNOWLEDGE_CHARS) {
                break;
            }
        }
        return sb.substring(0, Math.min(sb.length(), MAX_KNOWLEDGE_CHARS));
    }

    private Section indexSection() {
        for (Section s : sections) {
            if (s.title.contains(INDEX_SECTION_TITLE)) {
                return s;
            }
        }
        return null;
    }

    private void appendSection(StringBuilder sb, Section s) {
        if (s != null) {
            sb.append("### ").append(s.title).append('\n').append(s.body).append("\n\n");
        }
    }

    /** 打分：问题的 2-gram 在章节（标题权重 ×3）中出现次数 */
    private int score(Section s, List<String> ngrams) {
        int score = 0;
        for (String g : ngrams) {
            if (s.title.contains(g)) {
                score += 3;
            }
            if (s.body.contains(g)) {
                score += 1;
            }
        }
        return score;
    }

    /** 抽取问题中的中文/英文数字 2-gram（去停用短词由打分自然稀释） */
    private List<String> extractNgrams(String question) {
        String cleaned = question.replaceAll("[^\\p{IsHan}A-Za-z0-9]", "");
        List<String> grams = new ArrayList<>();
        for (int i = 0; i + 2 <= cleaned.length(); i++) {
            grams.add(cleaned.substring(i, i + 2));
        }
        return grams;
    }

    private static class Section {
        final String title;
        final String body;
        int score;

        Section(String title, String body) {
            this.title = title;
            this.body = body;
        }
    }
}
