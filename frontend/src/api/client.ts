const BASE_URL = '/api'

async function request<T>(
  path: string,
  options?: RequestInit,
): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    credentials: 'include',
    ...options,
  })

  if (res.status === 401 || res.status === 403) {
    window.location.href = '/'
    throw new Error('Unauthorized')
  }

  const data = await res.json() as T
  return data
}

export async function get<T>(path: string, params?: Record<string, string | number | undefined>): Promise<T> {
  let url = path
  if (params) {
    const filtered = Object.fromEntries(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== '' && v !== null)
    ) as Record<string, string>
    const qs = new URLSearchParams(filtered as Record<string, string>).toString()
    if (qs) url += '?' + qs
  }
  return request<T>(url)
}

export async function post<T>(path: string, body: Record<string, string | number>): Promise<T> {
  const formData = new URLSearchParams()
  for (const [k, v] of Object.entries(body)) {
    formData.append(k, String(v))
  }
  return request<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: formData.toString(),
  })
}
