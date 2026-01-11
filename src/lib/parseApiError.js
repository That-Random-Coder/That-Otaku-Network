export function parseApiErrorBody(body) {
  if (!body) return { message: null, fields: {} }
  if (Array.isArray(body) && body.length) {
    const fields = {}
    const msgs = body.map((it) => {
      const key = it?.keyError || it?.field
      const msg = it?.valueError || it?.message || (it?.keyError ? `${it.keyError}` : JSON.stringify(it))
      if (key) fields[key] = msg
      return msg
    })
    return { message: msgs.join(' • '), fields }
  }
  if (body?.message) return { message: body.message, fields: {} }
  if (typeof body === 'string') return { message: body, fields: {} }
  return { message: JSON.stringify(body), fields: {} }
}