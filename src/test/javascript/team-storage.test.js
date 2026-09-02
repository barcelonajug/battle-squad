const { describe, it, beforeEach } = require("node:test");
const assert = require("node:assert/strict");
const {
  teamStorage,
  TEAM_ID_STORAGE_KEY,
} = require("../../main/resources/static/js/team-storage.js");

describe("teamStorage", () => {
  let mockLocalStorage;
  let mockSessionStorage;

  function createMockStorage() {
    const store = new Map();
    return {
      getItem: (key) => store.get(key) ?? null,
      setItem: (key, value) => store.set(key, String(value)),
      removeItem: (key) => store.delete(key),
      clear: () => store.clear(),
    };
  }

  beforeEach(() => {
    mockLocalStorage = createMockStorage();
    mockSessionStorage = createMockStorage();
  });

  it("returns null when no team id is stored", () => {
    const result = teamStorage.getStoredTeamId(
      mockLocalStorage,
      mockSessionStorage,
    );
    assert.equal(result, null);
  });

  it("stores and retrieves team id from primary storage", () => {
    teamStorage.setStoredTeamId(
      "team-123",
      mockLocalStorage,
      mockSessionStorage,
    );
    assert.equal(mockLocalStorage.getItem(TEAM_ID_STORAGE_KEY), "team-123");
    assert.equal(
      teamStorage.getStoredTeamId(mockLocalStorage, mockSessionStorage),
      "team-123",
    );
  });

  it("clears team id from storage", () => {
    teamStorage.setStoredTeamId(
      "team-123",
      mockLocalStorage,
      mockSessionStorage,
    );
    teamStorage.clearStoredTeamId(mockLocalStorage, mockSessionStorage);
    assert.equal(
      teamStorage.getStoredTeamId(mockLocalStorage, mockSessionStorage),
      null,
    );
  });

  it("falls back to secondary storage when primary throws", () => {
    const failingStorage = {
      getItem: () => {
        throw new Error("SecurityError");
      },
      setItem: () => {
        throw new Error("QuotaExceeded");
      },
      removeItem: () => {
        throw new Error("SecurityError");
      },
    };
    mockSessionStorage.setItem(TEAM_ID_STORAGE_KEY, "fallback-team");
    const result = teamStorage.getStoredTeamId(
      failingStorage,
      mockSessionStorage,
    );
    assert.equal(result, "fallback-team");
  });

  it("falls back to secondary storage when saving and primary throws", () => {
    const failingStorage = {
      getItem: () => {
        throw new Error("SecurityError");
      },
      setItem: () => {
        throw new Error("QuotaExceeded");
      },
      removeItem: () => {
        throw new Error("SecurityError");
      },
    };
    teamStorage.setStoredTeamId(
      "fallback-saved",
      failingStorage,
      mockSessionStorage,
    );
    assert.equal(
      mockSessionStorage.getItem(TEAM_ID_STORAGE_KEY),
      "fallback-saved",
    );
  });

  it("handles both storages throwing without error", () => {
    const failingStorage = {
      getItem: () => {
        throw new Error("fail");
      },
      setItem: () => {
        throw new Error("fail");
      },
      removeItem: () => {
        throw new Error("fail");
      },
    };
    assert.doesNotThrow(() => {
      teamStorage.setStoredTeamId("team-abc", failingStorage, failingStorage);
      teamStorage.getStoredTeamId(failingStorage, failingStorage);
      teamStorage.clearStoredTeamId(failingStorage, failingStorage);
    });
  });
});
