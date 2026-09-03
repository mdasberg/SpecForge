class Component extends DCLogic {
  constructor(props) {
    super(props);
    this.state = {
      screen: 'home',
      tab: 'doc',
      modal: null,
      t1: false, t2: false, t4: true, t5: false,
      ai: 'open',
      composer: null,
      diff: 'inline',
      repoStep: 1,
      repoPick: null,
      repoSync: 'pr',
      connected: false,
    };
  }

  // repositories offered by the GitHub App installation
  repos() {
    const ready = { badge: 'Ready', badgeClass: 'b-approved', color: 'var(--green)' };
    const draft = { badge: 'Draft', badgeClass: 'b-changes', color: 'var(--amber)' };
    const tracked = { badge: 'Tracked', badgeClass: 'b-draft', color: 'var(--fg-3)' };
    return [
      {
        name: 'team-carepay/claim-management',
        project: 'Claim Management',
        team: 'Care platform',
        branch: 'main',
        path: 'openspec/specs/**/spec.md',
        domains: 'Claims, Adjudication',
        count: 5, reqs: 56, invalid: 1, versions: 34, proposals: 2,
        files: [
          { path: 'claims/pre-authorization/spec.md', note: '14 requirements', ...ready },
          { path: 'claims/adjudication/spec.md', note: '22 requirements', ...ready },
          { path: 'claims/intake/spec.md', note: '11 requirements', ...ready },
          { path: 'claims/reversal/spec.md', note: '9 requirements', ...ready },
          { path: 'claims/legacy-fax-flow/spec.md', note: 'no requirement ids', ...draft },
          { path: 'changes/cm-legacy-split/proposal.md', note: 'change proposal', ...tracked },
          { path: 'changes/cm-reversal-window/proposal.md', note: 'change proposal', ...tracked },
        ],
      },
      {
        name: 'team-carepay/askari',
        project: 'Askari',
        team: 'Architecture board',
        branch: 'main',
        path: 'openspec/specs/**/spec.md',
        domains: 'Authorization',
        count: 4, reqs: 54, invalid: 0, versions: 21, proposals: 1,
        files: [
          { path: 'authorization/relationship-model/spec.md', note: '18 requirements', ...ready },
          { path: 'authorization/permission-check/spec.md', note: '12 requirements', ...ready },
          { path: 'authorization/schema-bootstrap/spec.md', note: '8 requirements', ...ready },
          { path: 'authorization/token-exchange/spec.md', note: '16 requirements', ...ready },
          { path: 'changes/askari-caveats/proposal.md', note: 'change proposal', ...tracked },
        ],
      },
      {
        name: 'team-carepay/carepool-paging',
        project: 'Carepool Paging',
        team: 'Care platform',
        branch: 'main',
        path: 'openspec/specs/**/spec.md',
        domains: 'Platform',
        count: 3, reqs: 21, invalid: 0, versions: 9, proposals: 0,
        files: [
          { path: 'paging/response-headers/spec.md', note: '12 requirements', ...ready },
          { path: 'paging/slice-metadata/spec.md', note: '6 requirements', ...ready },
          { path: 'paging/sort-parameters/spec.md', note: '3 requirements', ...ready },
        ],
      },
    ];
  }

  repoVals(vals) {
    const { modal, repoStep: step, repoPick: pick, repoSync: sync } = this.state;
    const list = this.repos();
    const repo = list[pick ?? 0];

    vals.repoDisplay = modal === 'repo' ? 'contents' : 'none';
    vals.openRepo = () => this.setState({ modal: 'repo', repoStep: 1 });
    vals.closeRepo = () => this.setState({ modal: null });

    list.forEach((r, i) => {
      vals['repo' + i + 'Class'] = pick === i ? 'on' : '';
      vals['pickRepo' + i] = () => this.setState({ repoPick: i });
    });

    for (let s = 1; s <= 4; s++) vals['repoStep' + s + 'Display'] = step === s ? 'flex' : 'none';
    const stepClass = (n) => (step > n ? 'done' : step === n ? 'on' : '');
    vals.repoS1 = stepClass(1);
    vals.repoS2 = stepClass(2);
    vals.repoS3 = stepClass(3);

    const NEXT = {
      1: 'Continue',
      2: 'Continue',
      3: 'Connect repository',
      4: 'Go to the project',
    };
    const HINT = {
      1: 'SpecForge needs read access only — it never writes to the repository.',
      2: 'The path is a glob, so it can cover several directories.',
      3: 'All of this can be changed later in project settings.',
      4: 'You can connect another repository at any time.',
    };
    vals.repoNextLabel = NEXT[step];
    vals.repoHint = HINT[step];
    vals.repoNextClass = step === 1 && pick === null ? 'off' : '';
    vals.repoBackDisplay = step === 2 || step === 3 ? 'inline-flex' : 'none';
    vals.repoCancelDisplay = step === 4 ? 'none' : 'inline-flex';
    vals.repoBack = () => this.setState({ repoStep: Math.max(1, step - 1) });
    vals.repoNext = () => {
      if (step === 1 && pick === null) return;
      if (step === 3) return this.setState({ repoStep: 4, connected: true });
      if (step === 4) return this.setState({ modal: null, screen: 'projects' });
      this.setState({ repoStep: step + 1 });
    };

    vals.syncPrClass = sync === 'pr' ? 'on' : '';
    vals.syncPushClass = sync === 'push' ? 'on' : '';
    vals.syncManualClass = sync === 'manual' ? 'on' : '';
    vals.setSyncPr = () => this.setState({ repoSync: 'pr' });
    vals.setSyncPush = () => this.setState({ repoSync: 'push' });
    vals.setSyncManual = () => this.setState({ repoSync: 'manual' });

    vals.repoName = repo.name;
    vals.repoProject = repo.project;
    vals.repoTeam = repo.team;
    vals.repoBranch = repo.branch;
    vals.repoPath = repo.path;
    vals.repoDomains = repo.domains;
    vals.repoCount = repo.count;
    vals.repoImported = repo.count;
    vals.repoReqs = repo.reqs;
    vals.repoInvalid = repo.invalid;
    vals.repoVersions = repo.versions;
    vals.repoProposals = repo.proposals;
    vals.repoFiles = repo.files;
    vals.repoWarnDisplay = repo.invalid > 0 ? 'flex' : 'none';
    vals.repoCleanDisplay = repo.invalid > 0 ? 'none' : 'flex';
    vals.repoProposalsDisplay = repo.proposals > 0 ? 'flex' : 'none';
    vals.connectedDisplay = this.state.connected ? 'block' : 'none';
  }

  go(screen, extra) {
    return () => this.setState({ screen, ...extra });
  }

  tabGo(tab) {
    return () => this.setState({ screen: 'review', tab });
  }

  thread(key, vals) {
    const done = this.state[key];
    vals[key + 'Class'] = done ? 'resolved' : '';
    vals[key + 'Label'] = done ? 'Resolved' : 'Unresolved';
    vals[key + 'Btn'] = done ? 'Reopen' : 'Resolve';
    vals[key + 'Toggle'] = () => this.setState({ [key]: !done });
  }

  renderVals() {
    const { screen, tab, modal, ai, composer, diff } = this.state;
    const vals = { theme: this.props.theme ?? 'dark' };
    const show = (on) => (on ? 'contents' : 'none');
    const review = screen === 'review';

    // navigation
    vals.navHome = screen === 'home' ? 'on' : '';
    vals.navSpecs = screen === 'specs' ? 'on' : '';
    vals.navReviews = review ? 'on' : '';
    vals.navProjects = screen === 'projects' ? 'on' : '';
    vals.navActivity = screen === 'activity' ? 'on' : '';
    vals.openHome = this.go('home');
    vals.openSpecs = this.go('specs');
    vals.openProjects = this.go('projects');
    vals.openActivity = this.go('activity');
    vals.openReview = this.go('review');

    vals.homeDisplay = show(screen === 'home');
    vals.specsDisplay = show(screen === 'specs');
    vals.projectsDisplay = show(screen === 'projects');
    vals.activityDisplay = show(screen === 'activity');
    vals.specHeaderDisplay = review ? 'block' : 'none';

    // review tabs
    const TABS = ['doc', 'changes', 'discussions', 'checks', 'history', 'trace'];
    TABS.forEach((t) => {
      const key = t[0].toUpperCase() + t.slice(1);
      vals[t + 'Display'] = show(review && tab === t);
      vals['tab' + key + 'On'] = tab === t ? 'on' : '';
      vals['tab' + key] = this.tabGo(t);
    });
    vals.openChanges = this.tabGo('changes');
    vals.reviewNow = () => this.setState({ screen: 'review', tab: 'doc', composer: 'approve' });

    // discussion threads — state is shared by the document and the discussions tab
    ['t1', 't2', 't4', 't5'].forEach((k) => this.thread(k, vals));
    vals.unresolved = ['t1', 't2', 't4', 't5'].filter((k) => !this.state[k]).length
      + (ai === 'open' ? 1 : 0);
    vals.resolvedCount = 7 - vals.unresolved;

    // automated reviewer suggestion
    vals.t3Class = ai === 'open' ? '' : 'resolved';
    vals.aiOpenDisplay = ai === 'open' ? 'flex' : 'none';
    vals.aiDoneDisplay = ai === 'open' ? 'none' : 'flex';
    vals.aiStatusLabel = ai === 'open' ? 'Open' : ai === 'accepted' ? 'Accepted' : 'Dismissed';
    vals.aiDoneLabel = ai === 'accepted'
      ? 'Suggestion accepted — queued into v4 as AC-9.7'
      : 'Dismissed — recorded in history with your reason';
    vals.aiAccept = () => this.setState({ ai: 'accepted' });
    vals.aiDismiss = () => this.setState({ ai: 'dismissed' });
    vals.aiReset = () => this.setState({ ai: 'open' });

    // ticket modal
    vals.ticketDisplay = show(modal === 'ticket');
    vals.openTicket = () => this.setState({ modal: 'ticket' });
    vals.closeTicket = () => this.setState({ modal: null });
    vals.createTicket = () => this.setState({ modal: null, screen: 'review', tab: 'doc' });

    // change review mode
    const inline = diff === 'inline';
    vals.inlineDisplay = inline ? 'flex' : 'none';
    vals.sbsDisplay = inline ? 'none' : 'flex';
    vals.inlineTab = inline ? 'on' : '';
    vals.sbsTab = inline ? '' : 'on';
    vals.setInline = () => this.setState({ diff: 'inline' });
    vals.setSbs = () => this.setState({ diff: 'sbs' });

    // review verdict composer
    const copy = {
      approve: {
        title: 'Approve v3',
        body: 'Lifecycle and the 409 semantics look right. Approving on the understanding that CM-412 lands before implementation.',
        submit: 'Submit approval',
      },
      request: {
        title: 'Request changes on v3',
        body: 'The declined-line-item path in §4 needs a decision before this can be implemented.',
        submit: 'Submit review',
      },
      comment: {
        title: 'Comment on v3',
        body: 'Leaving a note without blocking: the expiry window change from 30 to 14 days needs a line in the release notes.',
        submit: 'Add comment',
      },
    };
    const active = copy[composer] ?? copy.approve;
    vals.composerDisplay = composer ? 'flex' : 'none';
    vals.composerTitle = active.title;
    vals.composerBody = active.body;
    vals.composerSubmit = active.submit;
    this.repoVals(vals);

    vals.openApprove = () => this.setState({ composer: 'approve' });
    vals.openRequest = () => this.setState({ composer: 'request' });
    vals.openComment = () => this.setState({ composer: 'comment' });
    vals.closeComposer = () => this.setState({ composer: null });

    return vals;
  }
}
