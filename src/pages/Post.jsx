import { useEffect, useState, useMemo } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import NavigationBar from '../components/NavigationBar.jsx'
import LoadingScreen from '../components/LoadingScreen.jsx'
import AlertBanner from '../components/AlertBanner.jsx'
import { ArrowLeft, ThumbsUp, MessageSquare, Share2, ThumbsDown } from 'lucide-react'
import accentOptions from '../theme/accentOptions.js'
import { getSavedAccentKey } from '../theme/accentStorage.js'
import DeletePostModal from '../components/DeletePostModal.jsx'

const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}

const formatDate = (iso) => {
  try {
    const d = new Date(iso)
    return d.toLocaleString()
  } catch (e) { return iso }
}

const buildMediaSrc = (media, mediaType) => {
  if (!media) return ''
  if (media.startsWith('data:')) return media
  const type = String(mediaType || '').toLowerCase()
  if (type.includes('image')) return `data:${mediaType || 'image/jpeg'};base64,${media}`
  if (type.includes('video')) return `data:${mediaType};base64,${media}`
  return media
}


const parseCountsFromResponse = (data) => {
  if (!data) return {}
  const item = Array.isArray(data) ? null : (data.data || data || {})
  const prefer = (k1, k2) => (item[k1] ?? item[k2] ?? data[k1] ?? data[k2])
  const likes = prefer('likeCount', 'likes')
  const dislikes = prefer('dislikeCount', 'dislikes')
  const commentCount = prefer('commentCount', 'comments')
  const shareCount = prefer('shareCount', 'shares')
  const isLiked = item.isLiked ?? item.liked ?? data.isLiked ?? data.liked
  const isDisliked = item.isDisliked ?? item.disliked ?? data.isDisliked ?? data.disliked
  const isFaved = item.isFaved ?? item.faved ?? data.isFaved ?? data.faved
  const result = {}
  if (typeof likes !== 'undefined') result.likeCount = Number(likes || 0)
  if (typeof dislikes !== 'undefined') result.dislikeCount = Number(dislikes || 0)
  if (typeof commentCount !== 'undefined') result.commentCount = Number(commentCount || 0)
  if (typeof shareCount !== 'undefined') result.shareCount = Number(shareCount || 0)
  if (typeof isLiked !== 'undefined') result.isLiked = Boolean(isLiked)
  if (typeof isDisliked !== 'undefined') result.isDisliked = Boolean(isDisliked)
  if (typeof isFaved !== 'undefined') result.isFaved = Boolean(isFaved)
  return result
}

export default function Post() {
  const { postId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const accentKey = getSavedAccentKey('crimson-night')
  const accent = useMemo(() => accentOptions.find((opt) => opt.key === accentKey)?.colors || accentOptions[0].colors, [accentKey])

  const [post, setPost] = useState(() => location.state?.post || null)
  const [loading, setLoading] = useState(() => !location.state?.post)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [pending, setPending] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [alertBanner, setAlertBanner] = useState(null)


  const COMMENTS_PAGE_SIZE = 12
  const [comments, setComments] = useState([])
  const [commentsPage, setCommentsPage] = useState(0)
  const [commentsLoading, setCommentsLoading] = useState(false)
  const [commentsHasMore, setCommentsHasMore] = useState(true)
  const [commentsError, setCommentsError] = useState('')
  const [commentText, setCommentText] = useState('')
  const [commentPosting, setCommentPosting] = useState(false)

  useEffect(() => {
    if (!postId) return
    let cancelled = false
    setLoading(true)
    setError('')

    const currentUserId = getCookie('id') || getCookie('userId') || ''
    const tokenRaw = getCookie('AccessToken') || localStorage.getItem('AccessToken') || ''
    const auth = tokenRaw.trim() ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''

    const url = `${import.meta.env.VITE_API_BASE_URL}content/content/get?contentId=${encodeURIComponent(postId)}&currentUserId=${encodeURIComponent(currentUserId)}&includeMedia=true`

    fetch(url, { headers: auth ? { Authorization: auth, AccessToken: tokenRaw } : {} })
      .then(async (res) => {
        const body = await res.json().catch(() => ({}))
        if (!res.ok) {
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
          throw new Error(parsed.message || 'Failed to fetch post')
        }
        return body
      })
      .then((data) => {
        if (cancelled) return
        setPost(data || null)
      })
      .catch((err) => {
        if (cancelled) return
        console.error('Post fetch error', err)
        setError(err.message || 'Failed to load post')
      })
      .finally(() => { if (!cancelled) setLoading(false) })

    return () => { cancelled = true }
  }, [postId])

  const toggleLike = async () => {
    if (!post || pending) return
    setPending(true)
    setActionError('')

    const prev = { ...post }

    setPost((p) => {
      const liked = !p.isLiked
      return {
        ...p,
        isLiked: liked,
        likeCount: Number(p.likeCount || 0) + (liked ? 1 : -1),
        isDisliked: liked ? false : p.isDisliked,
        dislikeCount: liked && p.isDisliked ? Math.max(0, Number(p.dislikeCount || 0) - 1) : p.dislikeCount,
      }
    })

    try {
      const tokenRaw = getCookie('AccessToken') || localStorage.getItem('AccessToken') || ''
      const auth = tokenRaw.trim() ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''
      const uid = getCookie('id') || getCookie('userId') || ''
      const url = import.meta.env.VITE_API_BASE_URL + 'content/content/like'
      const body = { contentId: post.id, userId: uid }
      try { console.log('[Post] liking URL:', url, 'body:', body, 'authPresent:', !!auth) } catch (e) {}

      const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', ...(auth ? { Authorization: auth, AccessToken: tokenRaw } : {}) }, body: JSON.stringify(body) })
      const data = await res.json().catch(() => ({}))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(data)
        throw new Error(parsed.message || 'Failed to like post')
      }


      const serverUpdate = parseCountsFromResponse(data)
      if (Object.keys(serverUpdate).length > 0) {
        setPost((p) => ({ ...p, ...serverUpdate }))
        window.dispatchEvent(new CustomEvent('post:updated', { detail: { contentId: post.id, update: { likes: serverUpdate.likeCount, dislikes: serverUpdate.dislikeCount, isLiked: serverUpdate.isLiked, isDisliked: serverUpdate.isDisliked } } }))
      }
    } catch (e) {
      console.error('like error', e)
      setPost(prev)
      setActionError(e.message || 'Failed to like')
      setTimeout(() => setActionError(''), 5000)
    } finally {
      setPending(false)
    }
  }

  const toggleDislike = async () => {
    if (!post || pending) return
    setPending(true)
    setActionError('')

    const prev = { ...post }
    setPost((p) => {
      const disliked = !p.isDisliked
      return {
        ...p,
        isDisliked: disliked,
        dislikeCount: Number(p.dislikeCount || 0) + (disliked ? 1 : -1),
        isLiked: disliked ? false : p.isLiked,
        likeCount: disliked && p.isLiked ? Math.max(0, Number(p.likeCount || 0) - 1) : p.likeCount,
      }
    })

    try {
      const tokenRaw = getCookie('AccessToken') || localStorage.getItem('AccessToken') || ''
      const auth = tokenRaw.trim() ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''
      const uid = getCookie('id') || getCookie('userId') || ''
      const url = import.meta.env.VITE_API_BASE_URL + 'content/content/dislike'
      const body = { contentId: post.id, userId: uid }
      try { console.log('[Post] disliking URL:', url, 'body:', body, 'authPresent:', !!auth) } catch (e) {}

      const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', ...(auth ? { Authorization: auth, AccessToken: tokenRaw } : {}) }, body: JSON.stringify(body) })
      const data = await res.json().catch(() => ({}))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(data)
        throw new Error(parsed.message || 'Failed to dislike post')
      }

      const serverUpdate = parseCountsFromResponse(data)
      if (Object.keys(serverUpdate).length > 0) {
        setPost((p) => ({ ...p, ...serverUpdate }))
        window.dispatchEvent(new CustomEvent('post:updated', { detail: { contentId: post.id, update: { likes: serverUpdate.likeCount, dislikes: serverUpdate.dislikeCount, isLiked: serverUpdate.isLiked, isDisliked: serverUpdate.isDisliked } } }))
      }
    } catch (e) {
      console.error('dislike error', e)
      setPost(prev)
      setActionError(e.message || 'Failed to dislike')
      setTimeout(() => setActionError(''), 5000)
    } finally {
      setPending(false)
    }
  }


  const fetchComments = async (page = 0) => {
    if (!post) return
    setCommentsLoading(true)
    setCommentsError('')
    try {
      const url = `${import.meta.env.VITE_API_BASE_URL}content/content/comments?contentId=${encodeURIComponent(post.id)}&page=${page}&size=${COMMENTS_PAGE_SIZE}`
      const tokenRaw = getCookie('AccessToken') || localStorage.getItem('AccessToken') || ''
      const auth = tokenRaw ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''
      try { console.log('[Post] fetching comments URL:', url, 'authPresent:', !!auth) } catch (e) {}
      const res = await fetch(url, { headers: { ...(auth ? { Authorization: auth, AccessToken: tokenRaw } : {}) } })
      const body = await res.json().catch(() => ({}))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
        throw new Error(parsed.message || 'Failed to fetch comments')
      }
      const arr = Array.isArray(body?.content) ? body.content : []
      if (page === 0) setComments(arr)
      else setComments((prev) => ([ ...prev, ...arr ]))
      const totalPages = Number(body?.totalPages || body?.pageable?.totalPages || 0)
      const number = Number(body?.number ?? body?.pageable?.pageNumber ?? page)
      const more = totalPages > 0 ? (number + 1 < Math.max(1, totalPages)) : (arr.length >= COMMENTS_PAGE_SIZE)
      setCommentsHasMore(more)
      setCommentsPage(number)
    } catch (e) {
      console.error('fetch comments error', e)
      setCommentsError(e.message || 'Failed to load comments')
    } finally {
      setCommentsLoading(false)
    }
  }


  useEffect(() => {
    setComments([])
    setCommentsPage(0)
    setCommentsHasMore(true)
    setCommentsError('')
    if (post) fetchComments(0)
  }, [post])


  useEffect(() => {
    const handler = (e) => {
      try {
        const { contentId, update } = e.detail || {}
        if (!contentId || !post || contentId !== post.id) return
        setPost((p) => ({ ...p, ...update }))
      } catch (err) { console.error('post:updated handler error', err) }
    }
    window.addEventListener('post:updated', handler)
    return () => window.removeEventListener('post:updated', handler)
  }, [post])

  if (loading) return <LoadingScreen key="loader-post" accent={accent} />

  if (error) return (
    <div className="min-h-screen text-slate-50" style={{ backgroundImage: `linear-gradient(135deg, ${accent.start}, ${accent.mid}, ${accent.end})` }}>
      <div className="relative z-10 px-4 pb-20 pt-14 sm:px-8 lg:px-12 xl:px-16">
        <div className="max-w-3xl mx-auto">
          <AlertBanner status="error" message={error} />
        </div>
      </div>
    </div>
  )

  if (!post) return null


  if (!post) return null

  const mediaSrc = buildMediaSrc(post.media, post.mediaType)

  const uid = getCookie('id') || getCookie('userId') || ''
  const ownerId = post?.userId ?? post?.userID ?? post?.ownerId ?? post?.creatorId ?? post?.user?.id ?? ''
  const isOwner = uid && ownerId && String(uid) === String(ownerId)

  const handleDeleteSuccess = (payload) => {
    setShowDeleteModal(false)
    setAlertBanner({ status: 'success', message: payload?.message || 'Post deleted' })

    setTimeout(() => navigate('/profile', { replace: true }), 700)
  }

  const postComment = async () => {
    if (!post || !commentText.trim() || commentPosting) return
    setCommentPosting(true)
    setCommentsError('')

    const uid = getCookie('id') || getCookie('userId') || ''
    const userName = getCookie('username') || getCookie('userName') || ''
    const displayName = getCookie('displayName') || ''
    const url = import.meta.env.VITE_API_BASE_URL + 'content/content/comment/add'
    const body = { contentId: post.id, userId: uid, userName: userName, comment: commentText }
    const tokenRaw = getCookie('AccessToken') || localStorage.getItem('AccessToken') || ''
    const auth = tokenRaw ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''
    try { console.log('[Post] adding comment URL:', url, 'body:', body, 'authPresent:', !!auth) } catch (e) {}


    const tmp = { id: `tmp-${Date.now()}`, comment: commentText, userId: uid, userName: userName, displayName: displayName || userName || 'You', commentAt: new Date().toISOString(), timeAgo: 'just now' }
    setComments((prev) => [ tmp, ...prev ])
    setCommentText('')

    try {
      const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', ...(auth ? { Authorization: auth, AccessToken: tokenRaw } : {}) }, body: JSON.stringify(body) })
      const data = await res.json().catch(() => ({}))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(data)
        throw new Error(parsed.message || 'Failed to add comment')
      }

      fetchComments(0)


      const serverUpdate = parseCountsFromResponse(data)
      if (Object.keys(serverUpdate).length > 0) {
        setPost((p) => ({ ...p, ...{ commentCount: serverUpdate.commentCount ?? serverUpdate.comments } }))
        window.dispatchEvent(new CustomEvent('post:updated', { detail: { contentId: post.id, update: { comments: serverUpdate.commentCount ?? serverUpdate.comments } } }))
      } else {

        setPost((p) => ({ ...p, commentCount: Number(p.commentCount || 0) + 1 }))
        window.dispatchEvent(new CustomEvent('post:updated', { detail: { contentId: post.id, update: { comments: (post.commentCount || 0) + 1 } } }))
      }
    } catch (e) {
      console.error('add comment error', e)

      setComments((prev) => prev.filter((c) => c.id !== tmp.id))
      setCommentsError(e.message || 'Failed to post comment')
      setTimeout(() => setCommentsError(''), 5000)
    } finally {
      setCommentPosting(false)
    }
  }

  return (
    <div className="relative min-h-screen overflow-visible text-slate-50" style={{ backgroundImage: `linear-gradient(135deg, ${accent.start}, ${accent.mid}, ${accent.end})` }}>
      <div className="pointer-events-none absolute inset-0" style={{ backgroundImage: `radial-gradient(circle at 45% 20%, ${accent.glow}, transparent 38%)`, mixBlendMode: 'screen' }} />
      <div className="relative z-10 px-4 pb-20 pt-14 sm:px-8 lg:px-12 xl:px-16">
        <div className="max-w-6xl mx-auto">
          <div className="flex items-center gap-3 mb-6">
            <button onClick={() => navigate(-1)} className="rounded-full border border-white/10 bg-white/5 p-2 text-slate-100 hover:bg-white/10 transition">
              <ArrowLeft className="h-4 w-4" />
            </button>
            <h1 className="text-2xl font-semibold">Post</h1>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-[min(58ch,60%)_1fr] gap-8">
            <div className="rounded-2xl overflow-hidden bg-black/10 border border-white/10 shadow-lg">
              {post.mediaType && String(post.mediaType).toLowerCase().includes('video') ? (
                <video src={mediaSrc} controls className="w-full max-h-[75vh] object-contain bg-black" />
              ) : (
                <img src={mediaSrc} alt={post.title || post.bio || 'Post media'} className="w-full max-h-[75vh] object-contain bg-black" />
              )}
            </div>

            <div className="flex flex-col gap-4">
<div
            className="flex items-center gap-3"
            {...(ownerId ? { role: 'button', tabIndex: 0, onClick: () => navigate(`/friend-profile/${encodeURIComponent(ownerId)}`), onKeyDown: (e) => { if (e.key === 'Enter') navigate(`/friend-profile/${encodeURIComponent(ownerId)}`) } } : {})}
          >
                <div className="h-12 w-12 rounded-full bg-black/30 flex items-center justify-center overflow-hidden text-xl font-bold text-slate-200/70">
                  {post.displayName ? post.displayName.slice(0,2).toUpperCase() : (post.userName || 'OT').slice(0,2).toUpperCase()}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-white">{post.displayName || post.userName}</span>
                    <span className="text-sm text-slate-300/80">@{post.userName}</span>
                  </div>
                  <div className="text-xs text-slate-400">{formatDate(post.created)}</div>
                </div>
              </div>

              {post.title && (
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-semibold text-white">{post.title}</h2>
                  {isOwner && (
                    <button type="button" onClick={() => setShowDeleteModal(true)} className="rounded-full border border-rose-400 bg-rose-600/5 text-rose-300 px-3 py-1.5 text-sm font-semibold hover:bg-rose-600/10 transition">Delete</button>
                  )}
                </div>
              )}
              {post.bio && <p className="text-slate-200/90">{post.bio}</p>}

              <div className="mt-2 flex items-center gap-4">
                <button disabled={pending} onClick={toggleLike} aria-label="Like post" className={`flex items-center gap-2 rounded-md px-3 py-2 transition ${post.isLiked ? 'bg-white/10' : 'bg-white/5'}`}>
                  <ThumbsUp className="h-5 w-5" />
                  <span>{post.likeCount || 0}</span>
                </button>

                <button disabled={pending} onClick={toggleDislike} aria-label="Dislike post" className={`flex items-center gap-2 rounded-md px-3 py-2 transition ${post.isDisliked ? 'bg-white/10' : 'bg-white/5'}`}>
                  <ThumbsDown className="h-5 w-5" />
                  <span>{post.dislikeCount || 0}</span>
                </button>

                <div className="flex items-center gap-2 rounded-md px-3 py-2 bg-white/5">
                  <MessageSquare className="h-5 w-5" />
                  <span>{post.commentCount || 0}</span>
                </div>

                <div className="flex items-center gap-2 rounded-md px-3 py-2 bg-white/5">
                  <Share2 className="h-5 w-5" />
                  <span>{post.shareCount || 0}</span>
                </div>
              </div>
              {actionError && <div className="mt-3"><AlertBanner status="error" message={actionError} /></div>}
              {alertBanner && <div className="mt-3"><AlertBanner status={alertBanner.status} message={alertBanner.message} /></div>}


              <DeletePostModal isOpen={showDeleteModal} onClose={() => setShowDeleteModal(false)} onSuccess={handleDeleteSuccess} contentId={post.id} accent={accent} />

            </div>
          </div>


          <div className="mt-8 max-w-6xl mx-auto">
            <div className="rounded-2xl border border-white/10 bg-white/5 p-6">
              <h3 className="text-lg font-semibold text-white mb-3">Comments ({post.commentCount || 0})</h3>

              <div className="mb-4">
                <textarea value={commentText} onChange={(e) => setCommentText(e.target.value)} placeholder="Add a comment..." className="w-full rounded-xl bg-black/10 border border-white/10 p-3 text-sm text-white placeholder:text-slate-400" rows={3} />
                <div className="mt-3 flex items-center justify-end gap-3">
                  <button onClick={() => { setCommentText('') }} disabled={commentPosting} className="rounded-md px-3 py-2 bg-white/5 text-sm">Cancel</button>
                  <button onClick={postComment} disabled={commentPosting || !commentText.trim()} className="rounded-md px-3 py-2 text-sm font-semibold text-white" style={{ backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` }}>{commentPosting ? 'Posting...' : 'Post'}</button>
                </div>
                {commentsError && <div className="mt-3"><AlertBanner status="error" message={commentsError} /></div>}
              </div>

              <div className="space-y-4">
                {comments.length === 0 && !commentsLoading && <div className="text-sm text-slate-300">No comments yet. Be the first to comment.</div>}
                {comments.map((c) => (
                  <div key={c.id} className="flex gap-3">
                    <div className="h-10 w-10 rounded-full bg-black/20 flex items-center justify-center text-sm font-semibold text-slate-200">{(c.displayName || c.userName || 'U').slice(0,2).toUpperCase()}</div>
                    <div className="flex-1">
                      <div className="flex items-center gap-3">
                        <span className="font-semibold text-white">{c.displayName || c.userName || c.userName}</span>
                        <span className="text-xs text-slate-400">{new Date(c.commentAt || Date.now()).toLocaleString()}</span>
                      </div>
                      <div className="mt-1 text-sm text-slate-200/90">{c.comment}</div>
                    </div>
                  </div>
                ))}

                {commentsLoading && <div className="text-sm text-slate-300">Loading comments...</div>}

                {commentsHasMore && !commentsLoading && (
                  <div className="mt-4 flex items-center justify-center">
                    <button onClick={() => fetchComments(commentsPage + 1)} className="rounded-md px-4 py-2 bg-white/5">Load more</button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
