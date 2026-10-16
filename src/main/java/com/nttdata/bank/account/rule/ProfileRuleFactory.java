package com.nttdata.bank.account.rule;

import com.nttdata.bank.account.model.CustomerProfile;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Returns the rule of each customer profile. */
@Component
public class ProfileRuleFactory {
  private final Map<CustomerProfile, ProfileRule> rules;

  /** Registers all the available profile rules. */
  public ProfileRuleFactory(List<ProfileRule> rules) {
    this.rules = rules.stream()
        .collect(Collectors.toMap(ProfileRule::profile, Function.identity()));
  }

  /** Gets the rule of a profile, standard when the profile is empty. */
  public ProfileRule get(CustomerProfile profile) {
    return rules.get(profile == null ? CustomerProfile.STANDARD : profile);
  }
}
