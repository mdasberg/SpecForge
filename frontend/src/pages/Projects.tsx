import { useEffect, useState } from 'react';
import { useAuth } from 'react-oidc-context';
import { listProjects } from '../api/catalog';
import type { ProjectCounts, ProjectSummary } from '../api/catalog';
import { ApiError } from '../auth/api';
import { EmptyState } from '../components/EmptyState';

const STATUS_META: { key: keyof ProjectCounts; label: string; color: string }[] = [
  { key: 'approved', label: 'approved', color: 'var(--green)' },
  { key: 'inReview', label: 'in review', color: 'var(--indigo)' },
  { key: 'changesRequested', label: 'changes requested', color: 'var(--amber)' },
  { key: 'implemented', label: 'implemented', color: 'var(--fg-3)' },
  { key: 'draft', label: 'draft', color: 'var(--line)' },
];

function ProjectCard({ project }: { project: ProjectSummary }) {
  const total = Math.max(project.specCount, 1);
  return (
    <div className="card">
      <div className="card-h">
        <span className="card-t">{project.name}</span>
        {project.team && <span className="tag">{project.team}</span>}
        <div className="spacer" />
        <span className="badge b-draft">{project.specCount} spec{project.specCount === 1 ? '' : 's'}</span>
      </div>
      <div className="card-pad">
        <div className="progress" style={{ marginBottom: 10 }}>
          {STATUS_META.map(({ key, color }) => {
            const count = project.counts[key];
            if (count === 0) return null;
            return <i key={key} style={{ width: `${(count / total) * 100}%`, background: color }} />;
          })}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14, fontSize: 11.5, flexWrap: 'wrap', marginBottom: 12 }}>
          {STATUS_META.map(({ key, label, color }) => {
            const count = project.counts[key];
            if (count === 0) return null;
            return (
              <span key={key} className="stat">
                <span style={{ color }}>{count}</span> {label}
              </span>
            );
          })}
        </div>
        <div className="kv">
          <dt>Repositories</dt>
          <dd className="mono">{project.repositories.join(', ')}</dd>
          <dt>Open reviews</dt>
          <dd>{project.openReviews}</dd>
        </div>
      </div>
    </div>
  );
}

export function Projects() {
  const auth = useAuth();
  const [projects, setProjects] = useState<ProjectSummary[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function run() {
      setLoading(true);
      setError(null);
      try {
        const result = await listProjects(auth.user);
        if (!cancelled) setProjects(result.items);
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError) {
          setError(e.problem.detail ?? e.problem.title ?? 'Could not load projects.');
        } else {
          throw e;
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void run();
    return () => {
      cancelled = true;
    };
  }, [auth.user]);

  if (loading) {
    return <div className="card card-pad">Loading projects…</div>;
  }

  if (error) {
    return (
      <div className="card card-pad">
        <div className="card-t">Could not load projects</div>
        <p style={{ color: 'var(--fg-2)', margin: '8px 0 0' }}>{error}</p>
      </div>
    );
  }

  if (!projects || projects.length === 0) {
    return (
      <EmptyState title="No projects yet">
        A project groups the specifications of one or more repositories. Connect a repository to
        create the first one.
      </EmptyState>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <h1 className="h1">Projects</h1>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 14 }}>
        {projects.map((project) => (
          <ProjectCard key={project.name} project={project} />
        ))}
      </div>
    </div>
  );
}
