import { useEffect, useRef, useState } from 'react';
import { useAuth } from 'react-oidc-context';
import { useNavigate } from 'react-router';
import { listNotifications, markNotificationRead } from '../api/discussion';
import type { Notification } from '../api/discussion';
import { formatRelativeTime } from '../lib/format';

const LABEL: Record<Notification['type'], string> = {
  MENTION: 'mentioned you',
  REPLY: 'replied on a thread you’re in',
};

/**
 * The caller's own notifications: a mention, or a reply on a thread they participate in. Polls
 * rather than pushing, the same tradeoff the rest of the app makes — there is no websocket
 * anywhere else in SpecForge, so this is not the place to introduce the first one.
 */
export function NotificationBell() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const boxRef = useRef<HTMLDivElement>(null);

  async function refresh() {
    if (!auth.isAuthenticated) return;
    const result = await listNotifications(auth.user);
    setItems(result.items);
    setUnreadCount(result.unreadCount);
  }

  useEffect(() => {
    void refresh();
    // ponytail: fixed poll rather than a push channel — see the class doc.
    const timer = globalThis.setInterval(() => void refresh(), 30_000);
    return () => globalThis.clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auth.isAuthenticated]);

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (open && boxRef.current && !boxRef.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, [open]);

  async function openNotification(notification: Notification) {
    if (!notification.readAt) {
      await markNotificationRead(notification.id, auth.user);
      await refresh();
    }
    setOpen(false);
    navigate(`/reviews/${notification.reviewId}?tab=discussions`);
  }

  if (!auth.isAuthenticated) return null;

  return (
    <div ref={boxRef} style={{ position: 'relative' }}>
      <button type="button" className="iconbtn" aria-label="Notifications" onClick={() => setOpen((v) => !v)}>
        {unreadCount > 0 ? '●' : '○'}
      </button>
      {unreadCount > 0 && (
        <span
          className="badge b-changes"
          style={{ position: 'absolute', top: -6, right: -6, height: 16, padding: '0 4px', fontSize: 9.5 }}
        >
          {unreadCount}
        </span>
      )}
      {open && (
        <div
          className="card"
          style={{ position: 'absolute', right: 0, top: 36, width: 320, maxHeight: 360, overflowY: 'auto', zIndex: 20 }}
        >
          <div className="rows">
            {items.length === 0 && <div className="row faint">No notifications yet.</div>}
            {items.map((notification) => (
              <button
                key={notification.id}
                type="button"
                className="row click"
                style={{ width: '100%', textAlign: 'left', border: 'none', background: 'none', font: 'inherit', opacity: notification.readAt ? 0.6 : 1 }}
                onClick={() => void openNotification(notification)}
              >
                <div style={{ minWidth: 0 }}>
                  <div className="row-t">
                    {notification.actor.displayName} {LABEL[notification.type]}
                  </div>
                  <div className="row-meta">{formatRelativeTime(notification.createdAt)}</div>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
