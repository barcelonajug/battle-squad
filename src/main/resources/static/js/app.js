$(document).ready(function () {

    let currentSessionId = null;
    let currentTeamId = null;
    let activeRoundNo = null;
    let currentRecommendation = null;

    // Pagination and Squad State
    let currentPage = 0;
    const pageSize = 12;
    let selectedHeroes = new Map(); // id -> hero object
    let roundConstraints = null;

    // Utility: Error Modal instead of alert
    function showError(message) {
        $('#error-modal-message').text(message);
        $('#error-modal-overlay').removeClass('error-modal-hidden').addClass('open');
    }

    $('#btn-close-error').on('click', function () {
        $('#error-modal-overlay').addClass('error-modal-hidden').removeClass('open');
    });

    // 1. Initial Load: Check session
    $.get('/api/contender/session', function (session) {
        if (session && session.active) {
            currentSessionId = session.sessionId;
            $('#session-badge').removeClass('hidden').text(`Session: ${session.sessionId.substring(0, 8)}...`);
            loadRounds(currentSessionId);
            loadHeroes(); // Initial fetch
        }
    }).fail(function () {
        console.log("No active session found immediately. Will pass null.");
    });

    // 2. Register Team
    $('#register-form').on('submit', function (e) {
        e.preventDefault();
        const name = $('#reg-team-name').val();
        const members = $('#reg-members').val().split(',').map(s => s.trim());

        const payload = {
            name,
            members,
            sessionId: currentSessionId
        };

        $.ajax({
            url: '/api/contender/register',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload),
            success: function (response) {
                currentTeamId = response.teamId;
                $('#team-id-display')
                    .html(`<strong>Team ID:</strong><br>${currentTeamId}`)
                    .removeClass('hidden');
                checkAIAvailability();
            },
            error: function (xhr) {
                showError("Registration failed: " + (xhr.responseJSON?.error || xhr.status));
            }
        });
    });

    // 3. Load Rounds List
    function loadRounds(sessionId) {
        $.get(`/api/contender/rounds?sessionId=${sessionId}`, function (rounds) {
            const select = $('#round-select');
            const currentValue = select.val();
            select.empty().append('<option value="">Select a round...</option>');
            rounds.forEach(r => {
                select.append(`<option value="${r.roundNo}">Round ${r.roundNo} (${r.status})</option>`);
            });
            if (currentValue) select.val(currentValue);

            $('#round-select').off('change').on('change', function () {
                const roundNo = $(this).val();
                if (roundNo) {
                    loadRoundConstraints(roundNo);
                } else {
                    $('#round-constraints-container').html('<p class="text-muted-foreground">Select a round to view constraints.</p>');
                    activeRoundNo = null;
                    roundConstraints = null;
                    updateSquadUI();
                    checkAIAvailability();
                }
            });
        });
    }

    $('#btn-refresh-rounds').on('click', function () {
        if (currentSessionId) loadRounds(currentSessionId);
    });

    // 4. Load Round Constraints
    function loadRoundConstraints(roundNo) {
        $.get(`/api/contender/round/${roundNo}?sessionId=${currentSessionId}`, function (spec) {
            activeRoundNo = parseInt(roundNo);
            roundConstraints = spec;
            checkAIAvailability();
            updateSquadUI();

            let rolesHtml = '';
            if (spec.requiredRoles) Object.entries(spec.requiredRoles).forEach(([k, v]) => rolesHtml += `<span class="bg-secondary px-2 py-0.5 rounded mr-1">${v}x ${k}</span>`);

            let bansHtml = '';
            if (spec.bannedTags) spec.bannedTags.forEach(t => bansHtml += `<span class="text-destructive bg-destructive/10 px-2 py-0.5 rounded mr-1">${t}</span>`);

            $('#round-constraints-container').html(`
                <div class="space-y-3">
                    <div><span class="font-semibold text-foreground">Mission:</span> ${spec.description}</div>
                    <div class="grid grid-cols-2 gap-2">
                        <div class="p-3 border border-border rounded-md bg-background">
                            <div class="text-muted-foreground text-xs uppercase mb-1">Budget Cap</div>
                            <div class="font-mono text-lg">${spec.budgetCap} 💰</div>
                        </div>
                        <div class="p-3 border border-border rounded-md bg-background">
                            <div class="text-muted-foreground text-xs uppercase mb-1">Team Size</div>
                            <div class="font-mono text-lg">${spec.teamSize} 🦸‍♂️</div>
                        </div>
                    </div>
                    ${rolesHtml ? `<div><div class="font-semibold text-foreground text-xs uppercase mb-1">Required Roles</div>${rolesHtml}</div>` : ''}
                    ${bansHtml ? `<div><div class="font-semibold text-foreground text-xs uppercase mb-1">Banned Tags</div>${bansHtml}</div>` : ''}
                </div>
            `);
        });
    }

    // 5. Hero Browser Search & List
    function loadHeroes() {
        const q = $('#hero-search').val();
        const alignment = $('#hero-alignment').val();

        $('#hero-list').html('<div class="flex justify-center py-8"><div class="loader w-8 h-8"></div></div>');

        const params = {
            page: currentPage,
            size: pageSize,
            q: q || undefined,
            alignment: alignment || undefined
        };

        $.get(`/api/contender/heroes`, params, function (heroes) {
            const list = $('#hero-list');
            list.empty();

            if (!heroes || heroes.length === 0) {
                list.html('<div class="text-center py-12 text-muted-foreground">No heroes found matching your criteria.</div>');
                $('#btn-next-page').prop('disabled', true);
                return;
            }

            // Simple "Next" button logic (disable if returned items < size)
            $('#btn-next-page').prop('disabled', heroes.length < pageSize);
            $('#page-info').text(`Page ${currentPage + 1}`);

            heroes.forEach(h => {
                const isChecked = selectedHeroes.has(h.id);
                list.append(`
                    <div class="hero-list-item flex items-center p-4 gap-4 cursor-pointer" data-hero-id="${h.id}">
                        <div class="flex-shrink-0">
                            <input type="checkbox" class="hero-checkbox w-5 h-5 rounded border-gray-300 text-primary focus:ring-primary" 
                                ${isChecked ? 'checked' : ''} data-hero-id="${h.id}">
                        </div>
                        <div class="w-12 h-12 rounded-full overflow-hidden bg-muted border border-border flex-shrink-0">
                            ${h.images?.sm ? `<img src="${h.images.sm}" class="w-full h-full object-cover">` : ''}
                        </div>
                        <div class="flex-1 min-w-0">
                            <h4 class="font-semibold text-sm truncate">${h.name}</h4>
                            <div class="text-xs text-muted-foreground">${h.role} | ${h.publisher || 'Unknown'}</div>
                        </div>
                        <div class="flex-shrink-0 text-right">
                            <div class="font-mono font-bold text-primary">${h.cost} 💰</div>
                            <div class="text-[10px] text-muted-foreground uppercase tracking-wider">${h.alignment}</div>
                        </div>
                    </div>
                `);
            });

            // Event listeners for hero elements
            $('.hero-list-item').on('click', function (e) {
                if ($(e.target).is('input')) return;
                const checkbox = $(this).find('.hero-checkbox');
                checkbox.prop('checked', !checkbox.prop('checked')).change();
            });

            $('.hero-checkbox').on('change', function (e) {
                const heroId = parseInt($(this).data('hero-id'));
                const hero = heroes.find(h => h.id === heroId);
                if ($(this).is(':checked')) {
                    selectedHeroes.set(heroId, hero);
                } else {
                    selectedHeroes.delete(heroId);
                }
                updateSquadUI();
            });
        });
    }

    $('#btn-search').on('click', function () {
        currentPage = 0;
        loadHeroes();
    });

    $('#btn-prev-page').on('click', function () {
        if (currentPage > 0) {
            currentPage--;
            loadHeroes();
            $('#btn-prev-page').prop('disabled', currentPage === 0);
        }
    });

    $('#btn-next-page').on('click', function () {
        currentPage++;
        loadHeroes();
        $('#btn-prev-page').prop('disabled', false);
    });

    // Trigger search on enter
    $('#hero-search').keypress(function (e) {
        if (e.which == 13) $('#btn-search').click();
    });

    // 6. Squad Management & UI
    function updateSquadUI() {
        const container = $('#selected-heroes');
        container.empty();

        let totalCost = 0;
        selectedHeroes.forEach((hero, id) => {
            totalCost += hero.cost;
            container.append(`
                <div class="flex items-center justify-between p-2 bg-secondary/50 rounded-md border border-border">
                    <div class="flex items-center gap-2">
                        <span class="text-xs font-semibold truncate max-w-[120px]">${hero.name}</span>
                        <span class="text-[10px] bg-primary/10 px-1 rounded truncate">${hero.cost}💰</span>
                    </div>
                    <button class="btn-remove-hero text-muted-foreground hover:text-destructive p-1" data-hero-id="${id}">
                        ✕
                    </button>
                </div>
            `);
        });

        if (selectedHeroes.size === 0) {
            container.html('<p class="text-sm text-muted-foreground italic">No heroes selected yet.</p>');
        }

        $('#total-squad-cost').text(`${totalCost} 💰`);

        const teamSize = roundConstraints ? roundConstraints.teamSize : '?';
        $('#squad-count').text(`${selectedHeroes.size} / ${teamSize}`);

        // Validation visual feedback
        if (roundConstraints) {
            if (totalCost > roundConstraints.budgetCap) {
                $('#total-squad-cost').addClass('text-destructive').removeClass('text-primary');
            } else {
                $('#total-squad-cost').removeClass('text-destructive').addClass('text-primary');
            }

            if (selectedHeroes.size > roundConstraints.teamSize) {
                $('#squad-count').addClass('bg-destructive/20 text-destructive').removeClass('bg-primary/10 text-primary');
            } else {
                $('#squad-count').removeClass('bg-destructive/20 text-destructive').addClass('bg-primary/10 text-primary');
            }
        }

        $('.btn-remove-hero').on('click', function () {
            const heroId = parseInt($(this).data('hero-id'));
            selectedHeroes.delete(heroId);

            // Uncheck in the list if visible
            $(`.hero-checkbox[data-hero-id="${heroId}"]`).prop('checked', false);

            updateSquadUI();
        });
    }

    // 6. AI Optimization Flow
    function checkAIAvailability() {
        if (currentTeamId && activeRoundNo && currentSessionId) {
            $('#btn-ai-optimize').prop('disabled', false);
        } else {
            $('#btn-ai-optimize').prop('disabled', true);
        }
    }

    const sheet = $('#ai-sheet');
    const overlay = $('#ai-sheet-overlay');

    function openSheet() {
        overlay.addClass('open');
        sheet.addClass('open');
    }

    function closeSheet() {
        overlay.removeClass('open');
        sheet.removeClass('open');
        $('#submit-feedback').hide();
    }

    $('#btn-close-sheet, #ai-sheet-overlay').on('click', closeSheet);

    $('#btn-ai-optimize').on('click', function () {
        const btn = $(this);
        btn.prop('disabled', true);
        btn.find('.loader').removeClass('hidden');
        btn.find('.btn-text').text('AI is thinking...');

        const payload = {
            teamId: currentTeamId,
            roundNo: activeRoundNo,
            sessionId: currentSessionId
        };

        $.ajax({
            url: '/api/contender/optimize',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload),
            success: function (resp) {
                currentRecommendation = resp;

                $('#ai-reasoning').text(resp.reasoning);
                $('#ai-strategy').text(resp.strategy);
                $('#ai-total-cost').text(`Cost: ${resp.totalCost}`);

                const heroesList = $('#ai-hero-ids');
                heroesList.empty();
                resp.heroes.forEach(h => {
                    heroesList.append(`<li>🦸‍♂️ ${h.name} <span class="text-xs ml-2 opacity-50">(ID: ${h.id})</span></li>`);
                });

                openSheet();
            },
            error: function (xhr) {
                showError("AI optimization failed: " + (xhr.responseJSON?.error || xhr.statusText));
            },
            complete: function () {
                btn.prop('disabled', false);
                btn.find('.loader').addClass('hidden');
                btn.find('.btn-text').text('Optimize with AI');
            }
        });
    });

    // 7. Submit Squad
    $('#btn-submit-squad').on('click', function () {
        if (!currentRecommendation) return;

        const btn = $(this);
        btn.prop('disabled', true).text('Submitting...');
        $('#submit-feedback').removeClass('bg-destructive/20 text-destructive bg-green-500/20 text-green-500').hide();

        const payload = {
            teamId: currentTeamId,
            roundNo: activeRoundNo,
            heroIds: currentRecommendation.heroes.map(h => h.id), // map the objects back to IDs
            strategy: currentRecommendation.strategy
        };

        $.ajax({
            url: '/api/contender/submit',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload),
            success: function () {
                $('#submit-feedback')
                    .text('✅ Squad submitted successfully to the Arena!')
                    .addClass('bg-green-500/20 text-green-400')
                    .show();
            },
            error: function (xhr) {
                $('#submit-feedback')
                    .text('❌ Submission failed: ' + (xhr.responseJSON?.error || xhr.statusText))
                    .addClass('bg-destructive/20 text-destructive')
                    .show();
            },
        });
    });

    // 8. Manual Squad Submission
    $('#btn-submit-manual-squad').on('click', function () {
        if (!currentTeamId) {
            showError("Please register your team first.");
            return;
        }
        if (!activeRoundNo) {
            showError("Please select a round first.");
            return;
        }
        if (selectedHeroes.size === 0) {
            showError("Your squad is empty. Please select at least one hero.");
            return;
        }

        // Optional: Front-end validation for team size and budget
        if (roundConstraints) {
            if (selectedHeroes.size > roundConstraints.teamSize) {
                showError(`Too many heroes! Max team size is ${roundConstraints.teamSize}.`);
                return;
            }
            let total = 0;
            selectedHeroes.forEach(h => total += h.cost);
            if (total > roundConstraints.budgetCap) {
                showError(`Budget exceeded! Max budget is ${roundConstraints.budgetCap}.`);
                return;
            }
        }

        const btn = $(this);
        btn.prop('disabled', true).text('Submitting...');

        const payload = {
            teamId: currentTeamId,
            roundNo: activeRoundNo,
            heroIds: Array.from(selectedHeroes.keys()),
            strategy: "Manually selected squad"
        };

        $.ajax({
            url: '/api/contender/submit',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload),
            success: function () {
                showError("✅ Squad submitted successfully to the Arena!");
                // Change color to green for success message if desired, but showError is a simple generic modal
            },
            error: function (xhr) {
                showError('❌ Submission failed: ' + (xhr.responseJSON?.error || xhr.statusText));
            },
            complete: function () {
                btn.prop('disabled', false).text('Submit Squad to Arena');
            }
        });
    });

});
