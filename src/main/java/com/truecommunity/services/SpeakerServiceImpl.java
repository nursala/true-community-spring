package com.truecommunity.services;

import com.truecommunity.dto.SpeakerForm;
import com.truecommunity.exception.SpeakerInUseException;
import com.truecommunity.exception.SpeakerNotFoundException;
import com.truecommunity.model.Speaker;
import com.truecommunity.repo.SpeakerRepository;
import com.truecommunity.repo.StatementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Transactional speaker-management implementation. It normalizes optional
 * fields, preserves statement references during edits, and rejects deletion
 * when a speaker is already used by any statement.
 */
@Service
@Transactional
public class SpeakerServiceImpl implements SpeakerService {

    private final SpeakerRepository speakerRepository;

    private final StatementRepository statementRepository;

    public SpeakerServiceImpl(SpeakerRepository speakerRepository,
                              StatementRepository statementRepository) {
        this.speakerRepository = speakerRepository;
        this.statementRepository = statementRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Speaker> getAllSpeakers() {
        return speakerRepository.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Speaker getById(Long id) {
        return speakerRepository.findById(id)
                .orElseThrow(() -> new SpeakerNotFoundException("Speaker not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Speaker> findByNameIgnoreCase(String name) {
        return speakerRepository.findByNameIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameIgnoreCase(String name) {
        return speakerRepository.existsByNameIgnoreCase(name);
    }

    /**
     * Creates a speaker after trimming required and optional fields.
     *
     * @param form the speaker form
     * @return the saved speaker
     */
    @Override
    public Speaker createSpeaker(SpeakerForm form) {
        Speaker speaker = Speaker.create(
                normalizeRequired(form.getName()),
                normalizeOptional(form.getRole()),
                normalizeOptional(form.getProfileUrl()),
                normalizeOptional(form.getImageUrl())
        );
        return speakerRepository.save(speaker);
    }

    /**
     * Updates editable speaker fields while keeping existing statement
     * relationships intact.
     *
     * @param id the speaker id
     * @param form the replacement speaker fields
     * @return the saved speaker
     */
    @Override
    public Speaker updateSpeaker(Long id, SpeakerForm form) {
        Speaker speaker = getById(id);
        speaker.setName(normalizeRequired(form.getName()));
        speaker.setRole(normalizeOptional(form.getRole()));
        speaker.setProfileUrl(normalizeOptional(form.getProfileUrl()));
        speaker.setImageUrl(normalizeOptional(form.getImageUrl()));
        return speakerRepository.save(speaker);
    }

    /**
     * Deletes a speaker only after verifying no statement references it.
     *
     * @param id the speaker id
     */
    @Override
    public void deleteSpeaker(Long id) {
        Speaker speaker = getById(id);
        if (statementRepository.existsBySpeaker_Id(id)) {
            throw new SpeakerInUseException("Speaker is referenced by existing statements.");
        }
        speakerRepository.delete(speaker);
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
