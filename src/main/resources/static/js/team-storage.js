const TEAM_ID_STORAGE_KEY = "battle_contender_team_id";

const teamStorage = {
  getStoredTeamId(
    primaryStorage = typeof localStorage !== "undefined" ? localStorage : null,
    secondaryStorage = typeof sessionStorage !== "undefined"
      ? sessionStorage
      : null,
  ) {
    try {
      const value = primaryStorage?.getItem(TEAM_ID_STORAGE_KEY);
      if (value) {
        return value;
      }
    } catch {}
    try {
      return secondaryStorage?.getItem(TEAM_ID_STORAGE_KEY) ?? null;
    } catch {
      return null;
    }
  },

  setStoredTeamId(
    teamId,
    primaryStorage = typeof localStorage !== "undefined" ? localStorage : null,
    secondaryStorage = typeof sessionStorage !== "undefined"
      ? sessionStorage
      : null,
  ) {
    try {
      primaryStorage?.setItem(TEAM_ID_STORAGE_KEY, teamId);
    } catch {
      try {
        secondaryStorage?.setItem(TEAM_ID_STORAGE_KEY, teamId);
      } catch {}
    }
  },

  clearStoredTeamId(
    primaryStorage = typeof localStorage !== "undefined" ? localStorage : null,
    secondaryStorage = typeof sessionStorage !== "undefined"
      ? sessionStorage
      : null,
  ) {
    try {
      primaryStorage?.removeItem(TEAM_ID_STORAGE_KEY);
    } catch {}
    try {
      secondaryStorage?.removeItem(TEAM_ID_STORAGE_KEY);
    } catch {}
  },
};

if (typeof module !== "undefined" && module.exports) {
  module.exports = {
    TEAM_ID_STORAGE_KEY,
    teamStorage,
  };
}
