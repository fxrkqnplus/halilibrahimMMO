package com.halilibrahim.halilibrahimmmo.guide;

import com.halilibrahim.halilibrahimmmo.core.PluginService;

import java.util.List;
import java.util.Optional;

/**
 * The in-game handbook behind {@code /yardım}, {@code /bilgi} and
 * {@code /rehber}.
 *
 * <p>The text lives in its own {@code rehber.yml} rather than in
 * {@code messages.yml}, and that is the same distinction every other catalogue
 * in this project already draws: {@code messages.yml} holds <em>templates</em>
 * the code fills in, while a quest's description, a skill's display name and a
 * guide page are <em>content</em>. Content belongs in a file a server owner can
 * rewrite, reorder and extend without a single Java constant having to exist
 * for each paragraph.</p>
 */
public interface GuideService extends PluginService {

    /** Every category, in the order {@code rehber.yml} declares them. */
    List<GuideCategory> categories();

    Optional<GuideCategory> find(String id);
}
