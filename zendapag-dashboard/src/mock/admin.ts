// @ts-nocheck
/**
 * ZENDPAG — PAINEL ADMIN MASTER
 * Dados mock (fake) realistas. O motor real entra depois; aqui é só para
 * dar vida às telas. Tudo em BRL, centralizado neste arquivo.
 */

export const brl = (value: number): string =>
  value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export const pct = (value: number): string =>
  `${value.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`;

// ───────────────────────────── DASHBOARD ─────────────────────────────

export const dashboardKpis = {
  pixProcessado: 2_847_320.55,
  pixVariacao: 12.4,
  cartaoProcessado: 1_932_180.0,
  cartaoVariacao: 8.1,
  boletoProcessado: 486_910.3,
  boletoVariacao: -3.2,
  documentosPendentes: 17,
  lucroTotal: 318_472.18,
  lucroVariacao: 15.7,
};

// Conversão geral (rosca)
export const conversaoGeral = [
  { nome: 'Aprovadas', valor: 8420, cor: '#10B981' },
  { nome: 'Recusadas', valor: 1180, cor: '#EF4444' },
  { nome: 'Pendentes', valor: 640, cor: '#F59E0B' },
];

// Conversão por adquirente
export const conversaoPorAdquirente = [
  { parceiro: 'Adiq', volume: 1_842_500, aprovacao: 94.2, falha: 5.8 },
  { parceiro: 'Cielo', volume: 1_210_300, aprovacao: 91.7, falha: 8.3 },
  { parceiro: 'Pagar.me', volume: 980_120, aprovacao: 89.4, falha: 10.6 },
  { parceiro: 'Stone', volume: 742_880, aprovacao: 92.9, falha: 7.1 },
  { parceiro: 'Rede', volume: 489_610, aprovacao: 87.5, falha: 12.5 },
];

// Faturamento por método (série mensal últimos 6 meses)
export const faturamentoPorMetodo = [
  { mes: 'Jan', pix: 1_980_000, cartao: 1_420_000, boleto: 410_000 },
  { mes: 'Fev', pix: 2_110_000, cartao: 1_510_000, boleto: 388_000 },
  { mes: 'Mar', pix: 2_340_000, cartao: 1_605_000, boleto: 432_000 },
  { mes: 'Abr', pix: 2_520_000, cartao: 1_720_000, boleto: 451_000 },
  { mes: 'Mai', pix: 2_690_000, cartao: 1_840_000, boleto: 470_000 },
  { mes: 'Jun', pix: 2_847_320, cartao: 1_932_180, boleto: 486_910 },
];

// Ranking de clientes (estabelecimentos por volume)
export const rankingClientes = [
  { nome: 'Loja Aurora Digital', documento: '12.345.678/0001-90', volume: 842_300, transacoes: 4120, conversao: 93.4 },
  { nome: 'EduMaster Cursos', documento: '98.765.432/0001-21', volume: 671_900, transacoes: 3380, conversao: 91.1 },
  { nome: 'FitShop Suplementos', documento: '45.612.378/0001-55', volume: 529_440, transacoes: 6210, conversao: 88.7 },
  { nome: 'TechParts BR', documento: '32.198.765/0001-12', volume: 487_120, transacoes: 2190, conversao: 90.2 },
  { nome: 'Bella Cosméticos', documento: '78.945.612/0001-33', volume: 392_680, transacoes: 5040, conversao: 86.5 },
  { nome: 'GamerZone Store', documento: '11.222.333/0001-44', volume: 318_210, transacoes: 1870, conversao: 89.9 },
];

// ─────────────────────────── ESTABELECIMENTOS ───────────────────────────

export type EstabStatus = 'ativo' | 'bloqueado' | 'analise' | 'restrito';
export type Compliance = 'aprovado' | 'pendente' | 'restrito' | 'reprovado';

export const estabelecimentosKpis = {
  total: 1284,
  ativos: 1107,
  emAnalise: 94,
  bloqueados: 83,
  saldoTotal: 14_982_310.44,
  saldoRetido: 1_204_870.0,
  documentosPendentes: 17,
};

export const estabelecimentos = [
  {
    id: 'EST-10245',
    nome: 'Loja Aurora Digital',
    razaoSocial: 'Aurora Comércio Digital LTDA',
    documento: '12.345.678/0001-90',
    tipo: 'PJ',
    email: 'financeiro@auroradigital.com.br',
    telefone: '(11) 98765-4321',
    status: 'ativo' as EstabStatus,
    compliance: 'aprovado' as Compliance,
    saldoDisponivel: 842_300.5,
    saldoPendente: 128_400.0,
    saldoRetido: 0,
    volumeMes: 842_300,
    medPct: 0.8,
    disputaPct: 0.4,
    retencaoPct: 0,
    adquirentes: ['Adiq', 'Cielo'],
    criadoEm: '2024-03-12',
  },
  {
    id: 'EST-10246',
    nome: 'EduMaster Cursos',
    razaoSocial: 'EduMaster Educação Online LTDA',
    documento: '98.765.432/0001-21',
    tipo: 'PJ',
    email: 'contato@edumaster.com.br',
    telefone: '(21) 99876-1234',
    status: 'ativo' as EstabStatus,
    compliance: 'aprovado' as Compliance,
    saldoDisponivel: 671_900.0,
    saldoPendente: 94_200.0,
    saldoRetido: 0,
    volumeMes: 671_900,
    medPct: 1.2,
    disputaPct: 0.7,
    retencaoPct: 5,
    adquirentes: ['Pagar.me', 'Stone'],
    criadoEm: '2023-11-28',
  },
  {
    id: 'EST-10247',
    nome: 'FitShop Suplementos',
    razaoSocial: 'FitShop Nutrição Esportiva LTDA',
    documento: '45.612.378/0001-55',
    tipo: 'PJ',
    email: 'adm@fitshop.com.br',
    telefone: '(31) 98123-7788',
    status: 'restrito' as EstabStatus,
    compliance: 'restrito' as Compliance,
    saldoDisponivel: 312_440.0,
    saldoPendente: 217_000.0,
    saldoRetido: 84_300.0,
    volumeMes: 529_440,
    medPct: 3.8,
    disputaPct: 2.1,
    retencaoPct: 15,
    adquirentes: ['Cielo'],
    criadoEm: '2024-01-05',
  },
  {
    id: 'EST-10248',
    nome: 'TechParts BR',
    razaoSocial: 'João Pedro Almeida ME',
    documento: '321.987.650-12',
    tipo: 'PF',
    email: 'joao@techparts.com.br',
    telefone: '(41) 99654-3210',
    status: 'ativo' as EstabStatus,
    compliance: 'aprovado' as Compliance,
    saldoDisponivel: 487_120.0,
    saldoPendente: 41_900.0,
    saldoRetido: 0,
    volumeMes: 487_120,
    medPct: 0.5,
    disputaPct: 0.3,
    retencaoPct: 0,
    adquirentes: ['Adiq', 'Rede'],
    criadoEm: '2024-05-19',
  },
  {
    id: 'EST-10249',
    nome: 'Bella Cosméticos',
    razaoSocial: 'Bella Beauty Comércio LTDA',
    documento: '78.945.612/0001-33',
    tipo: 'PJ',
    email: 'fiscal@bellacosmeticos.com.br',
    telefone: '(11) 97654-9087',
    status: 'analise' as EstabStatus,
    compliance: 'pendente' as Compliance,
    saldoDisponivel: 0,
    saldoPendente: 392_680.0,
    saldoRetido: 0,
    volumeMes: 392_680,
    medPct: 0,
    disputaPct: 0,
    retencaoPct: 0,
    adquirentes: [],
    criadoEm: '2025-06-10',
  },
  {
    id: 'EST-10250',
    nome: 'GamerZone Store',
    razaoSocial: 'GamerZone Comércio de Games LTDA',
    documento: '11.222.333/0001-44',
    tipo: 'PJ',
    email: 'suporte@gamerzone.com.br',
    telefone: '(51) 98432-1199',
    status: 'ativo' as EstabStatus,
    compliance: 'aprovado' as Compliance,
    saldoDisponivel: 318_210.0,
    saldoPendente: 28_700.0,
    saldoRetido: 0,
    volumeMes: 318_210,
    medPct: 1.0,
    disputaPct: 0.6,
    retencaoPct: 0,
    adquirentes: ['Stone'],
    criadoEm: '2024-08-02',
  },
  {
    id: 'EST-10251',
    nome: 'MegaImports Eletro',
    razaoSocial: 'MegaImports Importação LTDA',
    documento: '22.333.444/0001-55',
    tipo: 'PJ',
    email: 'financeiro@megaimports.com.br',
    telefone: '(11) 96543-2200',
    status: 'bloqueado' as EstabStatus,
    compliance: 'reprovado' as Compliance,
    saldoDisponivel: 0,
    saldoPendente: 0,
    saldoRetido: 198_420.0,
    volumeMes: 0,
    medPct: 6.4,
    disputaPct: 4.9,
    retencaoPct: 100,
    adquirentes: [],
    criadoEm: '2023-09-14',
  },
  {
    id: 'EST-10252',
    nome: 'Pet Amigo Online',
    razaoSocial: 'Maria Souza Comércio ME',
    documento: '456.789.120-33',
    tipo: 'PF',
    email: 'maria@petamigo.com.br',
    telefone: '(62) 98712-4455',
    status: 'ativo' as EstabStatus,
    compliance: 'aprovado' as Compliance,
    saldoDisponivel: 142_870.0,
    saldoPendente: 18_300.0,
    saldoRetido: 0,
    volumeMes: 142_870,
    medPct: 0.9,
    disputaPct: 0.5,
    retencaoPct: 0,
    adquirentes: ['Adiq'],
    criadoEm: '2025-02-21',
  },
];

// Adquirentes disponíveis para a aba do modal
export const adquirentesDisponiveis = ['Adiq', 'Cielo', 'Pagar.me', 'Stone', 'Rede'];

// ─────────────────────────────── TRANSAÇÕES ───────────────────────────────

export type TxMetodo = 'pix' | 'cartao' | 'boleto';
export type TxStatus = 'aprovada' | 'recusada' | 'pendente' | 'estornada' | 'disputa';

export const transacoesKpis = {
  volumeBruto: 5_266_410.85,
  taxaTotal: 184_324.4,
  volumeLiquido: 5_082_086.45,
  aprovadas: 8420,
  recusadas: 1180,
  ticketMedio: 312.4,
};

const motivosErro = [
  'Cartão sem limite disponível',
  'Cartão expirado',
  'Suspeita de fraude (antifraude)',
  'Dados do cartão inválidos',
  'Emissor indisponível (timeout)',
  'Saldo insuficiente',
];

export const transacoes = [
  {
    id: 'TX-8841290', tipo: 'geral', cliente: 'Carlos Henrique', documento: '123.456.789-00',
    estabelecimento: 'Loja Aurora Digital', metodo: 'pix' as TxMetodo, status: 'aprovada' as TxStatus,
    bruto: 459.9, taxa: 4.6, liquido: 455.3, adquirente: 'Adiq', motivoErro: null,
    criadoEm: '2026-06-23 14:32',
  },
  {
    id: 'TX-8841289', tipo: 'geral', cliente: 'Mariana Lopes', documento: '987.654.321-00',
    estabelecimento: 'EduMaster Cursos', metodo: 'cartao' as TxMetodo, status: 'aprovada' as TxStatus,
    bruto: 1299.0, taxa: 45.46, liquido: 1253.54, adquirente: 'Cielo', motivoErro: null,
    criadoEm: '2026-06-23 14:28',
  },
  {
    id: 'TX-8841288', tipo: 'geral', cliente: 'Roberto Dias', documento: '456.123.789-00',
    estabelecimento: 'FitShop Suplementos', metodo: 'cartao' as TxMetodo, status: 'recusada' as TxStatus,
    bruto: 289.9, taxa: 0, liquido: 0, adquirente: 'Cielo', motivoErro: motivosErro[0],
    criadoEm: '2026-06-23 14:21',
  },
  {
    id: 'TX-8841287', tipo: 'geral', cliente: 'Ana Beatriz', documento: '321.654.987-00',
    estabelecimento: 'TechParts BR', metodo: 'boleto' as TxMetodo, status: 'pendente' as TxStatus,
    bruto: 749.0, taxa: 3.49, liquido: 745.51, adquirente: 'Adiq', motivoErro: null,
    criadoEm: '2026-06-23 14:15',
  },
  {
    id: 'TX-8841286', tipo: 'geral', cliente: 'Felipe Moura', documento: '789.456.123-00',
    estabelecimento: 'Bella Cosméticos', metodo: 'cartao' as TxMetodo, status: 'recusada' as TxStatus,
    bruto: 159.9, taxa: 0, liquido: 0, adquirente: 'Pagar.me', motivoErro: motivosErro[2],
    criadoEm: '2026-06-23 14:09',
  },
  {
    id: 'TX-8841285', tipo: 'geral', cliente: 'Juliana Castro', documento: '654.987.321-00',
    estabelecimento: 'GamerZone Store', metodo: 'pix' as TxMetodo, status: 'aprovada' as TxStatus,
    bruto: 89.9, taxa: 0.9, liquido: 89.0, adquirente: 'Stone', motivoErro: null,
    criadoEm: '2026-06-23 13:58',
  },
  {
    id: 'TX-8841284', tipo: 'geral', cliente: 'Pedro Antunes', documento: '147.258.369-00',
    estabelecimento: 'Loja Aurora Digital', metodo: 'cartao' as TxMetodo, status: 'disputa' as TxStatus,
    bruto: 2150.0, taxa: 75.25, liquido: 2074.75, adquirente: 'Adiq', motivoErro: null,
    criadoEm: '2026-06-23 13:44',
  },
  {
    id: 'TX-8841283', tipo: 'geral', cliente: 'Camila Reis', documento: '258.369.147-00',
    estabelecimento: 'EduMaster Cursos', metodo: 'cartao' as TxMetodo, status: 'estornada' as TxStatus,
    bruto: 497.0, taxa: 17.4, liquido: -497.0, adquirente: 'Stone', motivoErro: null,
    criadoEm: '2026-06-23 13:30',
  },
  {
    id: 'TX-8841282', tipo: 'geral', cliente: 'Lucas Ferreira', documento: '369.147.258-00',
    estabelecimento: 'Pet Amigo Online', metodo: 'pix' as TxMetodo, status: 'aprovada' as TxStatus,
    bruto: 124.5, taxa: 1.25, liquido: 123.25, adquirente: 'Adiq', motivoErro: null,
    criadoEm: '2026-06-23 13:12',
  },
  {
    id: 'TX-8841281', tipo: 'geral', cliente: 'Beatriz Nunes', documento: '741.852.963-00',
    estabelecimento: 'TechParts BR', metodo: 'cartao' as TxMetodo, status: 'recusada' as TxStatus,
    bruto: 899.0, taxa: 0, liquido: 0, adquirente: 'Rede', motivoErro: motivosErro[4],
    criadoEm: '2026-06-23 12:55',
  },
  // Internas (transferências/ajustes entre contas da plataforma)
  {
    id: 'TX-INT-3391', tipo: 'interna', cliente: 'Zendpag — Ajuste de saldo', documento: '—',
    estabelecimento: 'FitShop Suplementos', metodo: 'pix' as TxMetodo, status: 'aprovada' as TxStatus,
    bruto: 84_300.0, taxa: 0, liquido: 84_300.0, adquirente: 'Interno', motivoErro: null,
    criadoEm: '2026-06-23 11:40',
  },
  {
    id: 'TX-INT-3390', tipo: 'interna', cliente: 'Zendpag — Liberação de retenção', documento: '—',
    estabelecimento: 'GamerZone Store', metodo: 'pix' as TxMetodo, status: 'aprovada' as TxStatus,
    bruto: 12_500.0, taxa: 0, liquido: 12_500.0, adquirente: 'Interno', motivoErro: null,
    criadoEm: '2026-06-23 10:18',
  },
  {
    id: 'TX-INT-3389', tipo: 'interna', cliente: 'Zendpag — Split afiliado', documento: '—',
    estabelecimento: 'EduMaster Cursos', metodo: 'pix' as TxMetodo, status: 'pendente' as TxStatus,
    bruto: 3_240.0, taxa: 0, liquido: 3_240.0, adquirente: 'Interno', motivoErro: null,
    criadoEm: '2026-06-23 09:02',
  },
];

// ──────────────────────────────── SAQUES ────────────────────────────────
// Visão do Admin Master: solicitações de saque de TODOS os estabelecimentos.
// O admin aprova, recusa ou acompanha o processamento.

export type SaqueMetodo = 'pix' | 'ted' | 'cripto';
export type SaqueStatus = 'pendente' | 'aprovado' | 'processando' | 'concluido' | 'recusado';
export type ChaveTipo = 'cpf' | 'cnpj' | 'email' | 'telefone' | 'aleatoria' | 'conta' | 'wallet';

export const saquesKpis = {
  solicitadoHoje: 487_320.0,
  pendentesQtd: 5,
  pendentesValor: 318_740.0,
  processandoQtd: 3,
  processandoValor: 96_180.0,
  concluidoMes: 4_812_990.0,
  taxasArrecadadas: 9_624.5,
};

const chaveLabel: Record<ChaveTipo, string> = {
  cpf: 'CPF',
  cnpj: 'CNPJ',
  email: 'E-mail',
  telefone: 'Telefone',
  aleatoria: 'Chave aleatória',
  conta: 'Conta bancária',
  wallet: 'Carteira',
};

export const chaveTipoLabel = (t: ChaveTipo): string => chaveLabel[t];

export const saques = [
  {
    id: 'SAQ-55210', estabelecimento: 'Loja Aurora Digital', documento: '12.345.678/0001-90',
    metodo: 'pix' as SaqueMetodo, chaveTipo: 'cnpj' as ChaveTipo, chave: '12.345.678/0001-90',
    bruto: 150_000.0, taxa: 0, liquido: 150_000.0, status: 'pendente' as SaqueStatus,
    prazo: 'Imediato', solicitadoEm: '2026-06-23 14:40', motivoRecusa: null,
  },
  {
    id: 'SAQ-55209', estabelecimento: 'FitShop Suplementos', documento: '45.612.378/0001-55',
    metodo: 'pix' as SaqueMetodo, chaveTipo: 'email' as ChaveTipo, chave: 'adm@fitshop.com.br',
    bruto: 84_300.0, taxa: 0, liquido: 84_300.0, status: 'pendente' as SaqueStatus,
    prazo: 'Imediato', solicitadoEm: '2026-06-23 14:12', motivoRecusa: null,
  },
  {
    id: 'SAQ-55208', estabelecimento: 'TechParts BR', documento: '321.987.650-12',
    metodo: 'ted' as SaqueMetodo, chaveTipo: 'conta' as ChaveTipo, chave: 'Banco 341 · Ag 1234 · CC 56789-0',
    bruto: 48_120.0, taxa: 12.9, liquido: 48_107.1, status: 'pendente' as SaqueStatus,
    prazo: 'D+1', solicitadoEm: '2026-06-23 13:50', motivoRecusa: null,
  },
  {
    id: 'SAQ-55207', estabelecimento: 'GamerZone Store', documento: '11.222.333/0001-44',
    metodo: 'pix' as SaqueMetodo, chaveTipo: 'aleatoria' as ChaveTipo, chave: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    bruto: 28_700.0, taxa: 0, liquido: 28_700.0, status: 'pendente' as SaqueStatus,
    prazo: 'Imediato', solicitadoEm: '2026-06-23 13:22', motivoRecusa: null,
  },
  {
    id: 'SAQ-55206', estabelecimento: 'Pet Amigo Online', documento: '456.789.120-33',
    metodo: 'cripto' as SaqueMetodo, chaveTipo: 'wallet' as ChaveTipo, chave: 'TRC20 · TJ8x...9hF2',
    bruto: 7_620.0, taxa: 38.1, liquido: 7_581.9, status: 'pendente' as SaqueStatus,
    prazo: '15-30 min', solicitadoEm: '2026-06-23 12:58', motivoRecusa: null,
  },
  {
    id: 'SAQ-55205', estabelecimento: 'EduMaster Cursos', documento: '98.765.432/0001-21',
    metodo: 'pix' as SaqueMetodo, chaveTipo: 'cnpj' as ChaveTipo, chave: '98.765.432/0001-21',
    bruto: 62_400.0, taxa: 0, liquido: 62_400.0, status: 'processando' as SaqueStatus,
    prazo: 'Imediato', solicitadoEm: '2026-06-23 11:30', motivoRecusa: null,
  },
  {
    id: 'SAQ-55204', estabelecimento: 'Bella Cosméticos', documento: '78.945.612/0001-33',
    metodo: 'ted' as SaqueMetodo, chaveTipo: 'conta' as ChaveTipo, chave: 'Banco 237 · Ag 0567 · CC 12345-6',
    bruto: 23_180.0, taxa: 12.9, liquido: 23_167.1, status: 'processando' as SaqueStatus,
    prazo: 'D+1', solicitadoEm: '2026-06-23 10:48', motivoRecusa: null,
  },
  {
    id: 'SAQ-55203', estabelecimento: 'Loja Aurora Digital', documento: '12.345.678/0001-90',
    metodo: 'pix' as SaqueMetodo, chaveTipo: 'cnpj' as ChaveTipo, chave: '12.345.678/0001-90',
    bruto: 10_600.0, taxa: 0, liquido: 10_600.0, status: 'processando' as SaqueStatus,
    prazo: 'Imediato', solicitadoEm: '2026-06-23 09:15', motivoRecusa: null,
  },
  {
    id: 'SAQ-55202', estabelecimento: 'EduMaster Cursos', documento: '98.765.432/0001-21',
    metodo: 'pix' as SaqueMetodo, chaveTipo: 'email' as ChaveTipo, chave: 'contato@edumaster.com.br',
    bruto: 94_200.0, taxa: 0, liquido: 94_200.0, status: 'concluido' as SaqueStatus,
    prazo: 'Imediato', solicitadoEm: '2026-06-22 17:04', motivoRecusa: null,
  },
  {
    id: 'SAQ-55201', estabelecimento: 'GamerZone Store', documento: '11.222.333/0001-44',
    metodo: 'pix' as SaqueMetodo, chaveTipo: 'telefone' as ChaveTipo, chave: '(51) 98432-1199',
    bruto: 41_800.0, taxa: 0, liquido: 41_800.0, status: 'concluido' as SaqueStatus,
    prazo: 'Imediato', solicitadoEm: '2026-06-22 15:39', motivoRecusa: null,
  },
  {
    id: 'SAQ-55200', estabelecimento: 'MegaImports Eletro', documento: '22.333.444/0001-55',
    metodo: 'pix' as SaqueMetodo, chaveTipo: 'cnpj' as ChaveTipo, chave: '22.333.444/0001-55',
    bruto: 198_420.0, taxa: 0, liquido: 198_420.0, status: 'recusado' as SaqueStatus,
    prazo: 'Imediato', solicitadoEm: '2026-06-22 14:10',
    motivoRecusa: 'Conta com bloqueio cautelar por índice de MED acima do limite.',
  },
  {
    id: 'SAQ-55199', estabelecimento: 'TechParts BR', documento: '321.987.650-12',
    metodo: 'cripto' as SaqueMetodo, chaveTipo: 'wallet' as ChaveTipo, chave: 'TRC20 · TQ2k...4mN8',
    bruto: 15_900.0, taxa: 79.5, liquido: 15_820.5, status: 'concluido' as SaqueStatus,
    prazo: '15-30 min', solicitadoEm: '2026-06-22 11:25', motivoRecusa: null,
  },
];

// ───────────────────── PAINEL UNIFICADO (RISCO) ─────────────────────
// Disputas (chargeback de cartão) + MEDs (Mecanismo Especial de Devolução
// do Pix) numa fila única, mais os bloqueios cautelares aplicados pelo
// Admin Master sobre estabelecimentos de risco.

export type RiscoTipo = 'med' | 'disputa';
export type RiscoStatus =
  | 'aberto'
  | 'em_analise'
  | 'contestado'
  | 'devolvido'
  | 'ganha'
  | 'perdida'
  | 'encerrado';

export const painelKpis = {
  medsAbertos: 5,
  medsValor: 96_840.0,
  disputasAbertas: 4,
  disputasValor: 41_790.0,
  taxaMedPlataforma: 1.4,
  bloqueiosAtivos: 3,
  saldoBloqueado: 480_940.0,
};

export const riscoItens = [
  {
    id: 'MED-7741', tipo: 'med' as RiscoTipo, estabelecimento: 'FitShop Suplementos', documento: '45.612.378/0001-55',
    cliente: 'Renata Aguiar', valor: 2_890.0, metodo: 'pix', adquirente: 'Cielo', txId: 'TX-8839102',
    motivo: 'Golpe/fraude relatada pelo recebedor', status: 'aberto' as RiscoStatus,
    prazoResposta: '2026-06-26', abertoEm: '2026-06-23 10:14',
  },
  {
    id: 'MED-7740', tipo: 'med' as RiscoTipo, estabelecimento: 'MegaImports Eletro', documento: '22.333.444/0001-55',
    cliente: 'Eduardo Pinto', valor: 38_400.0, metodo: 'pix', adquirente: 'Adiq', txId: 'TX-8838771',
    motivo: 'Cliente desconhece a transação', status: 'em_analise' as RiscoStatus,
    prazoResposta: '2026-06-25', abertoEm: '2026-06-22 16:40',
  },
  {
    id: 'MED-7739', tipo: 'med' as RiscoTipo, estabelecimento: 'GamerZone Store', documento: '11.222.333/0001-44',
    cliente: 'Paula Martins', valor: 1_290.0, metodo: 'pix', adquirente: 'Stone', txId: 'TX-8838540',
    motivo: 'Suspeita de coação', status: 'aberto' as RiscoStatus,
    prazoResposta: '2026-06-24', abertoEm: '2026-06-22 09:05',
  },
  {
    id: 'MED-7738', tipo: 'med' as RiscoTipo, estabelecimento: 'FitShop Suplementos', documento: '45.612.378/0001-55',
    cliente: 'Tiago Barros', valor: 4_560.0, metodo: 'pix', adquirente: 'Cielo', txId: 'TX-8837912',
    motivo: 'Golpe/fraude relatada pelo recebedor', status: 'devolvido' as RiscoStatus,
    prazoResposta: '2026-06-21', abertoEm: '2026-06-19 14:22',
  },
  {
    id: 'MED-7737', tipo: 'med' as RiscoTipo, estabelecimento: 'Loja Aurora Digital', documento: '12.345.678/0001-90',
    cliente: 'Sandra Lima', valor: 980.0, metodo: 'pix', adquirente: 'Adiq', txId: 'TX-8837004',
    motivo: 'Cliente desconhece a transação', status: 'contestado' as RiscoStatus,
    prazoResposta: '2026-06-24', abertoEm: '2026-06-21 11:48',
  },
  {
    id: 'DSP-4419', tipo: 'disputa' as RiscoTipo, estabelecimento: 'EduMaster Cursos', documento: '98.765.432/0001-21',
    cliente: 'Marcos Vieira', valor: 1_299.0, metodo: 'cartao', adquirente: 'Cielo', txId: 'TX-8836650',
    motivo: 'Não reconhece a compra', status: 'aberto' as RiscoStatus,
    prazoResposta: '2026-06-27', abertoEm: '2026-06-23 08:30',
  },
  {
    id: 'DSP-4418', tipo: 'disputa' as RiscoTipo, estabelecimento: 'Bella Cosméticos', documento: '78.945.612/0001-33',
    cliente: 'Larissa Gomes', valor: 459.9, metodo: 'cartao', adquirente: 'Pagar.me', txId: 'TX-8836120',
    motivo: 'Produto não recebido', status: 'em_analise' as RiscoStatus,
    prazoResposta: '2026-06-26', abertoEm: '2026-06-22 13:10',
  },
  {
    id: 'DSP-4417', tipo: 'disputa' as RiscoTipo, estabelecimento: 'TechParts BR', documento: '321.987.650-12',
    cliente: 'André Souza', valor: 899.0, metodo: 'cartao', adquirente: 'Rede', txId: 'TX-8835880',
    motivo: 'Cobrança duplicada', status: 'ganha' as RiscoStatus,
    prazoResposta: '2026-06-20', abertoEm: '2026-06-18 10:02',
  },
  {
    id: 'DSP-4416', tipo: 'disputa' as RiscoTipo, estabelecimento: 'MegaImports Eletro', documento: '22.333.444/0001-55',
    cliente: 'Fernanda Reis', valor: 2_150.0, metodo: 'cartao', adquirente: 'Adiq', txId: 'TX-8835411',
    motivo: 'Não reconhece a compra', status: 'perdida' as RiscoStatus,
    prazoResposta: '2026-06-19', abertoEm: '2026-06-17 15:33',
  },
];

export type BloqueioStatus = 'ativo' | 'liberado';

export const bloqueiosCautelares = [
  {
    id: 'BLQ-3012', estabelecimento: 'MegaImports Eletro', documento: '22.333.444/0001-55',
    motivo: 'Índice de MED em 6,4% (limite 3%) + disputa perdida de alto valor',
    saldoRetido: 198_420.0, medPct: 6.4, aplicadoEm: '2026-06-22 16:55', aplicadoPor: 'Admin Master',
    status: 'ativo' as BloqueioStatus,
  },
  {
    id: 'BLQ-3011', estabelecimento: 'FitShop Suplementos', documento: '45.612.378/0001-55',
    motivo: 'Picos de MED por golpe relatado; retenção preventiva durante apuração',
    saldoRetido: 84_300.0, medPct: 3.8, aplicadoEm: '2026-06-21 09:40', aplicadoPor: 'Carla Menezes',
    status: 'ativo' as BloqueioStatus,
  },
  {
    id: 'BLQ-3010', estabelecimento: 'Loja Aurora Digital', documento: '12.345.678/0001-90',
    motivo: 'Verificação de KYB após mudança de sócio',
    saldoRetido: 198_220.0, medPct: 0.8, aplicadoEm: '2026-06-20 11:15', aplicadoPor: 'Admin Master',
    status: 'ativo' as BloqueioStatus,
  },
  {
    id: 'BLQ-3009', estabelecimento: 'GamerZone Store', documento: '11.222.333/0001-44',
    motivo: 'Suspeita de chargeback em série — apuração concluída sem irregularidade',
    saldoRetido: 0, medPct: 1.0, aplicadoEm: '2026-06-15 14:00', aplicadoPor: 'Carla Menezes',
    status: 'liberado' as BloqueioStatus,
  },
];
