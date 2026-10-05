package com.truecommunity.services;

import com.truecommunity.dto.SpeakerForm;
import com.truecommunity.model.Speaker;

import java.util.List;
import java.util.Optional;

/**
 * Defines speaker lookup and management operations used by editor/admin pages
 * and statement creation.
 */
public interface SpeakerService {

    List<Speaker> getAllSpeakers();

    Speaker getById(Long id);

    Optional<Speaker> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    /**
     * Creates a speaker from the submitted management form.
     *
     * @param form the speaker fields
     * @return the saved speaker
     */
    Speaker createSpeaker(SpeakerForm form);

    /**
     * Updates an existing speaker without changing referenced statements.
     *
     * @param id the speaker id
     * @param form the replacement speaker fields
     * @return the saved speaker
     */
    Speaker updateSpeaker(Long id, SpeakerForm form);

    /**
     * Deletes a speaker only when no statement references it.
     *
     * @param id the speaker id
     */
    void deleteSpeaker(Long id);
}
