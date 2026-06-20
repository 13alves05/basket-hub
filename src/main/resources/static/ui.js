/**
 * BasketHub - Administração Avançada UI & Integração API
 */

const viewContainer = document.getElementById('view-container');
const viewTitle = document.getElementById('current-view-title');

// 1. Motor de Comunicação com Validação de Formato de Resposta (Anti-HTML Craches)
async function apiCall(url, method = 'GET', data = null) {
    const config = {
        method: method,
        headers:
            {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
    };
    if (data && (method === 'POST' || method === 'PUT')) {
        config.body = JSON.stringify(data);
    }

    try {
        const response = await fetch(url, config);
        const contentType = response.headers.get("content-type") || "";

        if (!response.ok) {
            const txt = await response.text();
            throw new Error(txt || `Código de estado do servidor: ${response.status}`);
        }

        // Se a resposta for HTML em vez de JSON, intercetar o erro estruturalmente
        if (contentType.includes("text/html")) {
            throw new Error("O servidor devolveu uma página HTML em vez de dados JSON. Verifique se o endpoint existe no Ktor.");
        }

        if (response.status === 204 || response.headers.get('content-length') === '0') {
            return null;
        }
        return await response.json();
    } catch (err) {
        console.error(`[API Call Fallback]:`, err);
        alert(`Operação mal-sucedida:\n${err.message}`);
        throw err;
    }
}

function showLoader() {
    viewContainer.innerHTML = `
        <div style="text-align:center; padding: 40px; color: var(--text-muted);">
            <i class="fa-solid fa-basketball fa-spin" style="font-size: 32px; margin-bottom: 12px; color: var(--primary);"></i>
            <p>A carregar dados do ecossistema...</p>
        </div>`;
}

// 2. Orquestrador de Alternância de Abas
function switchTab(tabName) {
    document.querySelectorAll('.nav-link').forEach(link => link.classList.remove('active'));
    const activeLink = document.querySelector(`[data-target="${tabName}"]`);
    if (activeLink) activeLink.classList.add('active');

    switch (tabName) {
        case 'dashboard':      loadDashboard(); break;
        case 'jogadores':      loadJogadores(); break;
        case 'equipas':        loadEquipas(); break;
        case 'eventos':        loadEventos(); break;
        case 'jogos':          loadJogos(); break;
        case 'pavilhoes':      loadPavilhoes(); break;
        case 'organizadores':  loadOrganizadores(); break;
        case 'patrocinadores': loadPatrocinadores(); break;
        case 'inscricoes':     loadInscricoes(); break;
        case 'classificacoes': loadClassificacoes(); break;
    }
}

// 3. Carregamento de Módulos com Seletores de Nomes (Sem IDsmanuais)

function loadDashboard() {
    viewTitle.innerText = "Dashboard";
    viewContainer.innerHTML = `
        <div class="grid-dashboard">
            <div class="card-stat"><div class="stat-details"><h3>Plataforma</h3><p>BasketHub</p></div><i class="fa-solid fa-basketball stat-icon" style="color:#f97316;"></i></div>
            <div class="card-stat"><div class="stat-details"><h3>Estado do Sistema</h3><p class="badge badge-success" style="font-size:16px; padding:6px 12px;">Operacional</p></div><i class="fa-solid fa-circle-check stat-icon" style="color:var(--success)"></i></div>
        </div>
        <div class="section-card">
            <div class="section-header"><h2>Gestão Central Desportiva</h2></div>
            <p style="color: var(--text-muted); line-height:1.6;">Selecione uma categoria no menu lateral para monitorizar o ecossistema e executar operações administrativas sem necessidade de chaves numéricas de bases de dados.</p>
        </div>
    `;
}

async function loadJogadores() {
    viewTitle.innerText = "Jogadores";
    showLoader();
    try {
        const dados = await apiCall('/jogadores');
        const rows = dados.map(j => `
            <tr>
                <td>${j.id}</td>
                <td><strong>${j.nome}</strong></td>
                <td><span class="badge">${j.posicao}</span></td>
                <td>#${j.n_camisola}</td>
                <td class="actions-cell">
                    <button class="btn-icon delete" onclick="deleteItem('/jogadores/${j.id}', loadJogadores)"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `).join('');

        viewContainer.innerHTML = `
            <div class="section-card">
                <div class="section-header"><h2>Registo de Atletas</h2></div>
                <form id="form-jogador" onsubmit="submitFormJogador(event)" style="margin-bottom:24px;">
                    <div class="flex-form-row">
                        <div class="modal-form-group"><label>Nome Completo</label><input type="text" id="j-nome" required></div>
                        <div class="modal-form-group"><label>Posição Campo</label><input type="text" id="j-posicao" placeholder="Ex: Base, Poste" required></div>
                        <div class="modal-form-group"><label>Nº Camisola</label><input type="number" id="j-camisola" min="0" max="99" required></div>
                    </div>
                    <button type="submit" class="btn"><i class="fa-solid fa-plus"></i> Adicionar Atleta</button>
                </form>
                <div class="table-responsive">
                    <table class="table-hub">
                        <thead><tr><th>ID</th><th>Nome</th><th>Posição</th><th>Nº Camisola</th><th>Ações</th></tr></thead>
                        <tbody>${rows || '<tr><td colspan="5" style="text-align:center;color:var(--text-muted);">Nenhum atleta listado.</td></tr>'}</tbody>
                    </table>
                </div>
            </div>`;
    } catch(e){}
}

async function loadEquipas() {
    viewTitle.innerText = "Equipas";
    showLoader();
    try {
        const dados = await apiCall('/equipas');
        const rows = dados.map(e => `
            <tr>
                <td>${e.id}</td>
                <td><strong>${e.nome}</strong></td>
                <td><span class="badge badge-primary">${e.sigla}</span></td>
                <td>${e.localidade}</td>
                <td class="actions-cell">
                    <button class="btn-icon delete" onclick="deleteItem('/equipas/${e.id}', loadEquipas)"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `).join('');

        viewContainer.innerHTML = `
            <div class="section-card">
                <div class="section-header"><h2>Registo de Clubes</h2></div>
                <form id="form-equipa" onsubmit="submitFormEquipa(event)" style="margin-bottom:24px;">
                    <div class="flex-form-row">
                        <div class="modal-form-group"><label>Nome da Equipa</label><input type="text" id="e-nome" required></div>
                        <div class="modal-form-group"><label>Sigla Oficial</label><input type="text" id="e-sigla" required></div>
                        <div class="modal-form-group"><label>Localidade</label><input type="text" id="e-localidade" required></div>
                    </div>
                    <button type="submit" class="btn"><i class="fa-solid fa-plus"></i> Criar Equipa</button>
                </form>
                <div class="table-responsive">
                    <table class="table-hub">
                        <thead><tr><th>ID</th><th>Nome</th><th>Sigla</th><th>Localidade</th><th>Ações</th></tr></thead>
                        <tbody>${rows || '<tr><td colspan="5" style="text-align:center;color:var(--text-muted);">Nenhuma equipa listada.</td></tr>'}</tbody>
                    </table>
                </div>
            </div>`;
    } catch(e){}
}

async function loadEventos() {
    viewTitle.innerText = "Eventos";
    showLoader();
    try {
        // Faz chamadas em paralelo para carregar as dependências de nomes reais
        const [dados, pavilhoes, organizadores] = await Promise.all([
            apiCall('/eventos'),
            apiCall('/pavilhoes'),
            apiCall('/organizadores')
        ]);

        const optPavilhoes = pavilhoes.map(p => `<option value="${p.id}">${p.nome} (${p.cidade})</option>`).join('');
        const optOrganizadores = organizadores.map(o => `<option value="${o.id}">${o.nome}</option>`).join('');

        const rows = dados.map(ev => {
            const pav = pavilhoes.find(p => p.id === ev.pavilhaoId)?.nome || `Pavilhão #${ev.pavilhaoId}`;
            const org = organizadores.find(o => o.id === ev.organizadorId)?.nome || `Supervisor #${ev.organizadorId}`;
            return `
            <tr>
                <td>${ev.id}</td>
                <td><strong>${ev.nome}</strong></td>
                <td>${ev.data}</td>
                <td>${pav}</td>
                <td>${org}</td>
                <td class="actions-cell">
                    <button class="btn" style="padding:6px 12px; font-size:12px;" onclick="actionPost('/eventos/${ev.id}/agendamento', loadEventos)"><i class="fa-solid fa-wand-magic-sparkles"></i> Sorteio</button>
                    <button class="btn-icon delete" onclick="deleteItem('/eventos/${ev.id}', loadEventos)"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>`;
        }).join('');

        viewContainer.innerHTML = `
            <div class="section-card">
                <div class="section-header"><h2>Gestão de Torneios e Competições</h2></div>
                <form id="form-evento" onsubmit="submitFormEvento(event)" style="margin-bottom:24px;">
                    <div class="flex-form-row">
                        <div class="modal-form-group"><label>Nome do Evento</label><input type="text" id="ev-nome" required></div>
                        <div class="modal-form-group"><label>Data</label><input type="text" id="ev-data" placeholder="AAAA-MM-DD" required></div>
                        <div class="modal-form-group"><label>Tipo de Torneio</label>
                            <select id="ev-type" onchange="toggleTematica(this.value)">
                                <option value="TORNEIO">Torneio Eliminatório</option>
                                <option value="AMIGAVEL">Amigável Individual</option>
                                <option value="CLINICA">Clínica de Formação</option>
                            </select>
                        </div>
                    </div>
                    <div class="flex-form-row">
                        <div class="modal-form-group"><label>Pavilhão Local</label><select id="ev-pavilhao">${optPavilhoes}</select></div>
                        <div class="modal-form-group"><label>Organizador Responsável</label><select id="ev-organizador">${optOrganizadores}</select></div>
                        <div class="modal-form-group" id="tematica-wrapper" style="display:none;"><label>Temática Pedagógica</label><input type="text" id="ev-tematica"></div>
                    </div>
                    <button type="submit" class="btn"><i class="fa-solid fa-plus"></i> Criar Evento Oficial</button>
                </form>
                <div class="table-responsive">
                    <table class="table-hub">
                        <thead><tr><th>ID</th><th>Nome</th><th>Data</th><th>Pavilhão</th><th>Organizador</th><th>Ações</th></tr></thead>
                        <tbody>${rows || '<tr><td colspan="6" style="text-align:center;color:var(--text-muted);">Nenhum evento agendado.</td></tr>'}</tbody>
                    </table>
                </div>
            </div>`;
    } catch(e){}
}

function toggleTematica(value) {
    document.getElementById('tematica-wrapper').style.display = value === 'CLINICA' ? 'block' : 'none';
}

async function loadJogos() {
    viewTitle.innerText = "Jogos";
    showLoader();
    try {
        const [dados, eventos, equipas, pavilhoes] = await Promise.all([
            apiCall('/jogos'),
            apiCall('/eventos'),
            apiCall('/equipas'),
            apiCall('/pavilhoes')
        ]);

        const optEventos = eventos.map(ev => `<option value="${ev.id}">${ev.nome}</option>`).join('');
        const optEquipas = equipas.map(e => `<option value="${e.id}">${e.nome}</option>`).join('');
        const optPavilhoes = pavilhoes.map(p => `<option value="${p.id}">${p.nome}</option>`).join('');

        const rows = dados.map(j => {
            const evNome = eventos.find(ev => ev.id === j.eventoId)?.nome || `Evento #${j.eventoId}`;
            const casaNome = equipas.find(e => e.id === j.equipaCasaId)?.nome || `Equipa #${j.equipaCasaId}`;
            const foraNome = j.equipaForaId ? (equipas.find(e => e.id === j.equipaForaId)?.nome || `Equipa #${j.equipaForaId}`) : 'ISENTO (BYE)';

            return `
            <tr>
                <td>${j.id}</td>
                <td>${evNome}</td>
                <td>Ronda ${j.ronda}</td>
                <td><strong>${casaNome}</strong> vs <strong>${foraNome}</strong></td>
                <td><span class="badge" style="font-weight:700;">${j.pontosCasa} - ${j.pontosFora}</span></td>
                <td>${j.dataHora}</td>
                <td class="actions-cell">
                    <button class="btn-icon delete" onclick="deleteItem('/jogos/${j.id}', loadJogos)"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>`;
        }).join('');

        viewContainer.innerHTML = `
            <div class="section-card">
                <div class="section-header"><h2>Resultados e Encontros Oficiais</h2></div>
                <form id="form-jogo" onsubmit="submitFormJogo(event)" style="margin-bottom:24px;">
                    <div class="flex-form-row">
                        <div class="modal-form-group"><label>Associação ao Evento</label><select id="jg-evento">${optEventos}</select></div>
                        <div class="modal-form-group"><label>Ronda Número</label><input type="number" id="jg-ronda" value="1" required></div>
                        <div class="modal-form-group"><label>Equipa Visitante (Casa)</label><select id="jg-casa">${optEquipas}</select></div>
                        <div class="modal-form-group"><label>Equipa Visitada (Fora)</label><select id="jg-fora"><option value="">-- Isento (BYE) --</option>${optEquipas}</select></div>
                    </div>
                    <div class="flex-form-row">
                        <div class="modal-form-group"><label>Local (Pavilhão)</label><select id="jg-pavilhao">${optPavilhoes}</select></div>
                        <div class="modal-form-group"><label>Pontuação Casa</label><input type="number" id="jg-ptscasa" value="0"></div>
                        <div class="modal-form-group"><label>Pontuação Fora</label><input type="number" id="jg-ptsfora" value="0"></div>
                        <div class="modal-form-group"><label>Data/Hora Legível</label><input type="text" id="jg-datahora" placeholder="Ex: Sábado, 16h" required></div>
                    </div>
                    <button type="submit" class="btn"><i class="fa-solid fa-plus"></i> Gravar Encontro</button>
                </form>
                <div class="table-responsive">
                    <table class="table-hub">
                        <thead><tr><th>ID</th><th>Evento</th><th>Ronda</th><th>Encontro</th><th>Resultado</th><th>Info</th><th>Ações</th></tr></thead>
                        <tbody>${rows || '<tr><td colspan="7" style="text-align:center;color:var(--text-muted);">Nenhum jogo registado neste torneio.</td></tr>'}</tbody>
                    </table>
                </div>
            </div>`;
    } catch(e){}
}

async function loadPavilhoes() {
    viewTitle.innerText = "Pavilhões";
    showLoader();
    try {
        const dados = await apiCall('/pavilhoes');
        const rows = dados.map(p => `
            <tr>
                <td>${p.id}</td>
                <td><strong>${p.nome}</strong></td>
                <td>${p.cidade}</td>
                <td>${p.localizacao}</td>
                <td>${p.capacidade.toLocaleString()}</td>
                <td class="actions-cell">
                    <button class="btn-icon delete" onclick="deleteItem('/pavilhoes/${p.id}', loadPavilhoes)"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `).join('');

        viewContainer.innerHTML = `
            <div class="section-card">
                <div class="section-header"><h2>Infraestruturas e Complexos Recintos</h2></div>
                <form id="form-pavilhao" onsubmit="submitFormPavilhao(event)" style="margin-bottom:24px;">
                    <div class="flex-form-row">
                        <div class="modal-form-group"><label>Nome do Complexo</label><input type="text" id="pv-nome" required></div>
                        <div class="modal-form-group"><label>Cidade</label><input type="text" id="pv-cidade" required></div>
                        <div class="modal-form-group"><label>Localização / Morada</label><input type="text" id="pv-localizacao" required></div>
                        <div class="modal-form-group"><label>Lotação Máxima</label><input type="number" id="pv-capacidade" required></div>
                    </div>
                    <button type="submit" class="btn"><i class="fa-solid fa-plus"></i> Adicionar Instalação</button>
                </form>
                <div class="table-responsive">
                    <table class="table-hub">
                        <thead><tr><th>ID</th><th>Nome</th><th>Cidade</th><th>Localização</th><th>Capacidade</th><th>Ações</th></tr></thead>
                        <tbody>${rows || '<tr><td colspan="6" style="text-align:center;color:var(--text-muted);">Nenhum pavilhão catalogado.</td></tr>'}</tbody>
                    </table>
                </div>
            </div>`;
    } catch(e){}
}

async function loadOrganizadores() {
    viewTitle.innerText = "Organizadores";
    showLoader();
    try {
        const dados = await apiCall('/organizadores');
        const rows = dados.map(o => `
            <tr>
                <td>${o.id}</td>
                <td><strong>${o.nome}</strong></td>
                <td>${o.contacto}</td>
                <td>${o.email}</td>
                <td class="actions-cell">
                    <button class="btn-icon delete" onclick="deleteItem('/organizador/${o.id}', loadOrganizadores)"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `).join('');

        viewContainer.innerHTML = `
            <div class="section-card">
                <div class="section-header"><h2>Corpo Diretivo de Organizadores</h2></div>
                <form id="form-organizador" onsubmit="submitFormOrganizador(event)" style="margin-bottom:24px;">
                    <div class="flex-form-row">
                        <div class="modal-form-group"><label>Nome Completo</label><input type="text" id="org-nome" required></div>
                        <div class="modal-form-group"><label>Contacto Telefónico</label><input type="text" id="org-contacto" required></div>
                        <div class="modal-form-group"><label>E-mail Corporativo</label><input type="email" id="org-email" required></div>
                    </div>
                    <button type="submit" class="btn"><i class="fa-solid fa-plus"></i> Homologar Supervisor</button>
                </form>
                <div class="table-responsive">
                    <table class="table-hub">
                        <thead><tr><th>ID</th><th>Nome</th><th>Contacto</th><th>Email</th><th>Ações</th></tr></thead>
                        <tbody>${rows || '<tr><td colspan="5" style="text-align:center;color:var(--text-muted);">Nenhum organizador adicionado.</td></tr>'}</tbody>
                    </table>
                </div>
            </div>`;
    } catch(e){}
}

async function loadPatrocinadores() {
    viewTitle.innerText = "Patrocinadores";
    showLoader();
    try {
        const [dados, eventos] = await Promise.all([
            apiCall('/patrocinadores'),
            apiCall('/eventos')
        ]);

        const optEventos = eventos.map(ev => `<option value="${ev.id}">${ev.nome}</option>`).join('');

        const rows = dados.map(p => {
            const evNome = p.eventoId ? (eventos.find(ev => ev.id === p.eventoId)?.nome || `Evento #${p.eventoId}`) : 'Fundo Geral Desportivo';
            return `
            <tr>
                <td>${p.id}</td>
                <td><strong>${p.nome}</strong></td>
                <td>${p.empresa}</td>
                <td><strong style="color:var(--primary);">${p.valorContrato.toLocaleString('pt-PT', { style: 'currency', currency: 'EUR' })}</strong></td>
                <td><span class="badge">${evNome}</span></td>
                <td class="actions-cell">
                    <button class="btn-icon delete" onclick="deleteItem('/patrocinadores/${p.id}', loadPatrocinadores)"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>`;
        }).join('');

        viewContainer.innerHTML = `
            <div class="section-card">
                <div class="section-header"><h2>Parcerias Comerciais e Patrocínios</h2></div>
                <form id="form-patrocinador" onsubmit="submitFormPatrocinador(event)" style="margin-bottom:24px;">
                    <div class="flex-form-row">
                        <div class="modal-form-group"><label>Nome do Representante</label><input type="text" id="pat-nome" required></div>
                        <div class="modal-form-group"><label>Empresa / Marca</label><input type="text" id="pat-empresa" required></div>
                        <div class="modal-form-group"><label>Montante do Contrato (€)</label><input type="number" step="0.01" id="pat-valor" required></div>
                        <div class="modal-form-group"><label>Alocação de Competição</label>
                            <select id="pat-evento">
                                <option value="">-- Fundo de Apoio Geral --</option>
                                ${optEventos}
                            </select>
                        </div>
                    </div>
                    <button type="submit" class="btn"><i class="fa-solid fa-plus"></i> Registar Acordo Comercial</button>
                </form>
                <div class="table-responsive">
                    <table class="table-hub">
                        <thead><tr><th>ID</th><th>Nome Representante</th><th>Empresa</th><th>Contrato Financeiro</th><th>Vínculo Competitivo</th><th>Ações</th></tr></thead>
                        <tbody>${rows || '<tr><td colspan="6" style="text-align:center;color:var(--text-muted);">Nenhum patrocinador ativo.</td></tr>'}</tbody>
                    </table>
                </div>
            </div>`;
    } catch(e) { console.error("Erro ao carregar patrocinadores", e); }
}

async function loadInscricoes() {
    viewTitle.innerText = "Inscrições";
    showLoader();
    try {
        const [dados, equipas, eventos] = await Promise.all([
            apiCall('/inscricoes'),
            apiCall('/equipas'),
            apiCall('/eventos')
        ]);

        const optEquipas = equipas.map(e => `<option value="${e.id}">${e.nome}</option>`).join('');
        const optEventos = eventos.map(ev => `<option value="${ev.id}">${ev.nome}</option>`).join('');

        const rows = dados.map(i => {
            const eqNome = equipas.find(e => e.id === i.equipaId)?.nome || `Equipa #${i.equipaId}`;
            const evNome = eventos.find(ev => ev.id === i.eventoId)?.nome || `Evento #${i.eventoId}`;
            return `
            <tr>
                <td>${i.id}</td>
                <td><strong>${eqNome}</strong></td>
                <td>${evNome}</td>
                <td>${i.dataInscricao}</td>
                <td class="actions-cell">
                    <button class="btn-icon delete" onclick="deleteItem('/inscricoes/${i.id}', loadInscricoes)"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>`;
        }).join('');

        viewContainer.innerHTML = `
            <div class="section-card">
                <div class="section-header"><h2>Inscrições Federadas em Competições</h2></div>
                <form id="form-inscricao" onsubmit="submitFormInscricao(event)" style="margin-bottom:24px;">
                    <div class="flex-form-row">
                        <div class="modal-form-group"><label>Clube a Inscrever</label><select id="ins-equipa">${optEquipas}</select></div>
                        <div class="modal-form-group"><label>Torneio de Destino</label><select id="ins-evento">${optEventos}</select></div>
                        <div class="modal-form-group"><label>Data da Matrícula</label><input type="text" id="ins-data" placeholder="AAAA-MM-DD" required></div>
                    </div>
                    <button type="submit" class="btn"><i class="fa-solid fa-plus"></i> Finalizar Matrícula</button>
                </form>
                <div class="table-responsive">
                    <table class="table-hub">
                        <thead><tr><th>ID</th><th>Equipa</th><th>Evento</th><th>Data Registo</th><th>Ações</th></tr></thead>
                        <tbody>${rows || '<tr><td colspan="5" style="text-align:center;color:var(--text-muted);">Nenhuma inscrição registada.</td></tr>'}</tbody>
                    </table>
                </div>
            </div>`;
    } catch(e){}
}

async function loadClassificacoes() {
    viewTitle.innerText = "Classificações";
    showLoader();
    try {
        const [dados, equipas] = await Promise.all([
            apiCall('/classificacoes/geral'),
            apiCall('/equipas')
        ]);

        const rows = dados.map((c, index) => {
            const eqNome = equipas.find(e => e.id === c.equipaId)?.nome || `Equipa #${c.equipaId}`;
            return `
            <tr>
                <td><strong>${index + 1}º</strong></td>
                <td><strong>${eqNome}</strong></td>
                <td>${c.jogos}</td>
                <td>${c.vitorias}</td>
                <td>${c.derrotas}</td>
                <td><code>${c.pontosMarcados}:${c.pontosSofridos}</code></td>
                <td>${c.saldo > 0 ? '+' + c.saldo : c.saldo}</td>
                <td><span class="badge badge-success">${c.pontos} Pts</span></td>
            </tr>`;
        }).join('');

        viewContainer.innerHTML = `
            <div class="section-card">
                <div class="section-header"><h2>Tabela Classificativa Líderes Geral</h2></div>
                <div class="table-responsive">
                    <table class="table-hub">
                        <thead><tr><th>Pos</th><th>Equipa</th><th>J</th><th>V</th><th>D</th><th>Marcados:Sofridos</th><th>+/-</th><th>Pontuação</th></tr></thead>
                        <tbody>${rows || '<tr><td colspan="8" style="text-align:center;color:var(--text-muted);">Nenhum dado computado para gerar tabelas.</td></tr>'}</tbody>
                    </table>
                </div>
            </div>`;
    } catch(e){}
}

// 4. Intercetores de Submissão Corporativa de Formulários

async function submitFormJogador(e) {
    e.preventDefault();
    await apiCall('/jogadores', 'POST', {
        nome: document.getElementById('j-nome').value,
        posicao: document.getElementById('j-posicao').value,
        n_camisola: parseInt(document.getElementById('j-camisola').value, 10)
    });
    loadJogadores();
}

async function submitFormEquipa(e) {
    e.preventDefault();
    await apiCall('/equipas', 'POST', {
        nome: document.getElementById('e-nome').value,
        sigla: document.getElementById('e-sigla').value,
        localidade: document.getElementById('e-localidade').value
    });
    loadEquipas();
}

async function submitFormEvento(e) {
    e.preventDefault();
    const type = document.getElementById('ev-type').value;
    const bodyData = {
        type: type,
        nome: document.getElementById('ev-nome').value,
        data: document.getElementById('ev-data').value,
        pavilhaoId: parseInt(document.getElementById('ev-pavilhao').value, 10),
        organizadorId: parseInt(document.getElementById('ev-organizador').value, 10)
    };
    if (type === 'CLINICA') {
        bodyData.tematica = document.getElementById('ev-tematica').value;
    }
    await apiCall('/eventos', 'POST', bodyData);
    loadEventos();
}

async function submitFormJogo(e) {
    e.preventDefault();

    // Obter valores dos inputs
    const eventoId = parseInt(document.getElementById('jg-evento').value, 10);
    const ronda = parseInt(document.getElementById('jg-ronda').value, 10);
    const casaId = parseInt(document.getElementById('jg-casa').value, 10);
    const foraVal = document.getElementById('jg-fora').value;
    const foraId = foraVal ? parseInt(foraVal, 10) : null;
    const pavilhaoId = parseInt(document.getElementById('jg-pavilhao').value, 10);
    const pontosCasa = parseInt(document.getElementById('jg-ptscasa').value, 10);
    const pontosFora = parseInt(document.getElementById('jg-ptsfora').value, 10);
    const dataHora = document.getElementById('jg-datahora').value;

    // 1. Validação: Impedir o mesmo clube como casa e fora
    if (foraId !== null && casaId === foraId) {
        alert("Erro: A equipa da casa não pode ser a mesma equipa visitante.");
        return;
    }

    // 2. Validação: Impedir empates (Regra de Basquetebol)
    // Nota: Apenas validamos se a equipa de fora existir (não for 'bye')
    if (foraId !== null && pontosCasa === pontosFora) {
        alert("Erro: O resultado não pode terminar empatado. Verifique os pontos.");
        return;
    }

    // Envio para a API
    try {
        await apiCall('/jogos', 'POST', {
            eventoId,
            ronda,
            equipaCasaId: casaId,
            equipaForaId: foraId,
            pavilhaoId,
            pontosCasa,
            pontosFora,
            dataHora
        });
        loadJogos();
    } catch (err) {
        // O apiCall já dispara o alert, aqui apenas paramos a execução
    }
}

async function submitFormPavilhao(e) {
    e.preventDefault();
    await apiCall('/pavilhoes', 'POST', {
        nome: document.getElementById('pv-nome').value,
        cidade: document.getElementById('pv-cidade').value,
        localizacao: document.getElementById('pv-localizacao').value,
        capacidade: parseInt(document.getElementById('pv-capacidade').value, 10)
    });
    loadPavilhoes();
}

async function submitFormOrganizador(e) {
    e.preventDefault();
    await apiCall('/organizadores', 'POST', {
        nome: document.getElementById('org-nome').value,
        contacto: document.getElementById('org-contacto').value,
        email: document.getElementById('org-email').value
    });
    loadOrganizadores();
}

async function submitFormPatrocinador(e) {
    e.preventDefault();
    const evVal = document.getElementById('pat-evento').value;
    await apiCall('/patrocinadores', 'POST', {
        nome: document.getElementById('pat-nome').value,
        empresa: document.getElementById('pat-empresa').value,
        valorContrato: parseFloat(document.getElementById('pat-valor').value),
        eventoId: evVal ? parseInt(evVal, 10) : null
    });
    loadPatrocinadores();
}

async function submitFormInscricao(e) {
    e.preventDefault();
    await apiCall('/inscricoes', 'POST', {
        equipaId: parseInt(document.getElementById('ins-equipa').value, 10),
        eventoId: parseInt(document.getElementById('ins-evento').value, 10),
        dataInscricao: document.getElementById('ins-data').value
    });
    loadInscricoes();
}

// 5. Destruição Segura de Elementos

async function deleteItem(url, callback) {
    if (confirm('Deseja realmente eliminar este item permanentemente do sistema?')) {
        await fetch(url, { method: 'DELETE' });
        callback();
    }
}

async function actionPost(url, callback) {
    try {
        const response = await fetch(url, { method: 'POST' });
        if (!response.ok) throw new Error("Falha no agendamento");
        alert("Sorteio/Agendamento realizado com sucesso!");
        callback();
    } catch (err) {
        alert("Erro na operação de agendamento: " + err.message);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (viewContainer) loadDashboard();
});