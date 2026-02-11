package com.weekly.service;

import com.weekly.entity.PromptTemplate;
import com.weekly.entity.PromptType;
import com.weekly.repository.PromptTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateService.class);

    private final PromptTemplateRepository repository;

    public PromptTemplateService(PromptTemplateRepository repository) {
        this.repository = repository;
    }

    public List<PromptTemplate> findAll() {
        return repository.findAllByOrderByPromptTypeAscNameAsc();
    }

    public List<PromptTemplate> findByType(PromptType type) {
        return repository.findByPromptTypeOrderByNameAsc(type);
    }

    public PromptTemplate findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("템플릿을 찾을 수 없습니다: " + id));
    }

    public String getPrompt(PromptType type) {
        return repository.findByPromptTypeAndIsDefaultTrue(type)
                .map(PromptTemplate::getContent)
                .orElse(getBuiltInPrompt(type));
    }

    /**
     * 특정 템플릿 ID가 지정되면 해당 템플릿을, 아니면 기본 템플릿을 반환.
     */
    public String getPrompt(Long templateId, PromptType fallbackType) {
        if (templateId != null) {
            return repository.findById(templateId)
                    .map(PromptTemplate::getContent)
                    .orElse(getPrompt(fallbackType));
        }
        return getPrompt(fallbackType);
    }

    @Transactional
    public PromptTemplate create(PromptTemplate template) {
        if (template.isDefault()) {
            clearDefaultForType(template.getPromptType());
        }
        return repository.save(template);
    }

    @Transactional
    public PromptTemplate update(Long id, PromptTemplate updates) {
        PromptTemplate template = findById(id);
        template.setName(updates.getName());
        template.setPromptType(updates.getPromptType());
        template.setContent(updates.getContent());
        if (updates.isDefault() && !template.isDefault()) {
            clearDefaultForType(updates.getPromptType());
        }
        template.setDefault(updates.isDefault());
        return repository.save(template);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void setDefault(Long id) {
        PromptTemplate template = findById(id);
        clearDefaultForType(template.getPromptType());
        template.setDefault(true);
        repository.save(template);
    }

    private void clearDefaultForType(PromptType type) {
        repository.findByPromptTypeAndIsDefaultTrue(type).ifPresent(existing -> {
            existing.setDefault(false);
            repository.save(existing);
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaults() {
        if (repository.count() > 0) return;
        log.info("Seeding default prompt templates...");

        createSeed("기본 일반보고", PromptType.GENERAL, true, DEFAULT_GENERAL_PROMPT);
        createSeed("기본 팀통합", PromptType.TEAM_AGGREGATE, true, DEFAULT_TEAM_AGGREGATE_PROMPT);
    }

    private void createSeed(String name, PromptType type, boolean isDefault, String content) {
        PromptTemplate t = new PromptTemplate();
        t.setName(name);
        t.setPromptType(type);
        t.setDefault(isDefault);
        t.setContent(content);
        repository.save(t);
    }

    private String getBuiltInPrompt(PromptType type) {
        return switch (type) {
            case GENERAL -> DEFAULT_GENERAL_PROMPT;
            case TEAM_AGGREGATE -> DEFAULT_TEAM_AGGREGATE_PROMPT;
        };
    }

    static final String DEFAULT_GENERAL_PROMPT = """
            당신은 주간업무보고를 정리하는 비서입니다.
            사용자가 자연어로 입력한 한 주간의 업무 내용을 마크다운 형식으로 구조화하세요.

            반드시 아래 예시와 동일한 마크다운 문법을 사용하세요:

            ```markdown
            ### 이번 주 수행 업무
            - **[업무명]**: 상세 수행 내용 (완료/진행중 표기)
            - **[업무명]**: 상세 수행 내용 (완료/진행중 표기)

            ### 다음 주 계획
            - **[업무명]**: 계획 상세 내용
            - **[업무명]**: 계획 상세 내용

            ### 이슈사항
            - **[이슈명]**: 상세 내용 및 대응 방안
            ```

            규칙:
            1. 반드시 ### 으로 시작하는 헤더를 사용할 것
            2. 반드시 - 로 시작하는 불릿 리스트를 사용할 것
            3. 업무명은 반드시 **[업무명]** 형태(볼드+대괄호)로 표기할 것
            4. 완료된 업무는 "(완료)", 진행 중인 업무는 "(진행중)"을 붙일 것
            5. 사용자가 언급한 업무를 빠짐없이 포함할 것
            6. 다음 주 계획이 명시적으로 언급되지 않았으면, 진행 중인 업무의 후속 작업을 추론하여 작성할 것
            7. 이슈가 없으면 "- 특이사항 없음"으로 표기할 것
            8. 입력이 짧거나 불완전해도 거부하지 말고, 위 마크다운 형식에 맞게 정리할 것
            9. ### 헤더, - 불릿, **볼드** 외에 다른 안내 문구나 설명을 출력하지 말 것
            """;

    static final String DEFAULT_TEAM_AGGREGATE_PROMPT = """
            당신은 팀 주간보고를 종합하는 비서입니다.
            여러 팀원이 각각 작성한 주간보고 내용을 하나의 팀 주간보고로 종합하세요.

            반드시 아래 예시와 동일한 마크다운 문법을 사용하세요:

            ```markdown
            ### 팀 주간 현황 요약
            - 핵심 성과와 진행 상황을 3~5개 불릿으로 요약

            ### 팀원별 주요 업무
            | 팀원 | 주요 수행 업무 | 다음 주 계획 |
            |------|----------------|--------------|
            | 홍길동 | 업무 내용 요약 | 계획 내용 요약 |

            ### 팀 이슈 및 협의 필요사항
            - **[이슈명]**: 상세 내용 및 대응 방안
            ```

            규칙:
            1. 반드시 ### 으로 시작하는 헤더를 사용할 것
            2. 반드시 - 로 시작하는 불릿 리스트를 사용할 것
            3. 팀원별 업무는 반드시 마크다운 테이블(| 구분자) 형식으로 작성할 것
            4. 모든 팀원의 내용을 빠짐없이 반영할 것
            5. 중복 업무는 합쳐서 표현할 것
            6. 이슈가 없으면 "- 특이사항 없음"으로 표기할 것
            7. ### 헤더, - 불릿, 테이블 외에 다른 안내 문구나 설명을 출력하지 말 것
            """;

}
