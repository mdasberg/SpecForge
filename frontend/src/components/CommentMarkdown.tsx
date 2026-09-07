import ReactMarkdown from 'react-markdown';
import type { Components } from 'react-markdown';
import remarkGfm from 'remark-gfm';

/**
 * A comment body, rendered with the same markdown pipeline {@link SpecMarkdown} uses for
 * specification text, minus the heading-anchor logic a comment never needs.
 *
 * <p>`mentions` is the set of handles the backend already resolved against real members; a
 * `@handle` in the body is only linkified when it is in that set, which is what keeps the mention
 * of a non-member rendering as plain text rather than a dead link.
 *
 * <p>ponytail: the rewrite runs on the raw string before markdown parses it, so an `@handle` that
 * happens to sit inside a code span would be rewritten too. Comments are prose, not code, so this
 * is not worth a markdown-aware pass unless it turns out to bite.
 */
export function CommentMarkdown({ body, mentions }: { body: string; mentions: string[] }) {
  const linked = mentions.reduce(
    (text, handle) => text.split(`@${handle}`).join(`[@${handle}](mention:${handle})`),
    body,
  );

  const components: Components = {
    a({ href, children }) {
      if (href?.startsWith('mention:')) {
        return <span className="mention">{children}</span>;
      }
      return (
        <a href={href} target="_blank" rel="noreferrer">
          {children}
        </a>
      );
    },
  };

  return (
    <div className="cmt-t">
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
        {linked}
      </ReactMarkdown>
    </div>
  );
}
