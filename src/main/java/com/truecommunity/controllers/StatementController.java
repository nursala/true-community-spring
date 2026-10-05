package com.truecommunity.controllers;

import com.truecommunity.dto.StatementSearchForm;
import com.truecommunity.dto.StatementResponse;
import com.truecommunity.model.Statement;
import com.truecommunity.model.Topic;
import com.truecommunity.services.StatementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for public statement lookup and search endpoints.
 */
@RestController
@RequestMapping("/api/statements")
public class StatementController {

    private final StatementService statementService;

    public StatementController(StatementService statementService) {
        this.statementService = statementService;
    }

    /**
     * Returns one statement by id.
     *
     * @param id statement id
     * @return statement response or {@code 404 Not Found}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getStatement(@PathVariable Long id) {
        return statementService.findById(id)
                .map(this::toStatementResponse)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Returns pending statements for API consumers.
     *
     * @return pending statement responses
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingStatements() {
        return ResponseEntity.ok(toStatementResponses(statementService.getPendingStatements()));
    }

    /**
     * Searches statements using the shared search form.
     *
     * @param form search criteria
     * @return matching statement responses
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(@Valid @ModelAttribute StatementSearchForm form) {
        return ResponseEntity.ok(toStatementResponses(statementService.search(form)));
    }

    private List<StatementResponse> toStatementResponses(List<Statement> statements) {
        return statements.stream()
                .map(this::toStatementResponse)
                .toList();
    }

    private StatementResponse toStatementResponse(Statement statement) {
        StatementResponse response = new StatementResponse();
        response.setId(statement.getId());
        response.setText(statement.getText());
        response.setSpeakerName(statement.getSpeakerName());
        response.setSourceUrl(statement.getSourceUrl());
        response.setTopics(statement.getTopics().stream()
                .map(Topic::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        response.setStatus(statement.getStatus());
        response.setCreatedAt(statement.getCreatedAt());
        response.setPublishedAt(statement.getPublishedAt());
        return response;
    }
}
