<template>
  <div class="page">
    <header class="page-header">
      <div>
        <div class="eyebrow">控制台总览</div>
        <h1 class="page-title">桌宠运行态势</h1>
        <p class="page-subtitle">面向实时设备与指令执行的轻量操作台，覆盖状态、遥测与操作闭环。</p>
      </div>
      <div class="header-actions">
        <button class="btn primary">新建指令</button>
        <button class="btn ghost">刷新遥测</button>
      </div>
    </header>

    <section class="metrics-grid">
      <div class="card">
        <div class="metric-value">18</div>
        <div class="metric-label">在线设备</div>
        <div class="metric-note">近 5 分钟活跃</div>
      </div>
      <div class="card">
        <div class="metric-value">42</div>
        <div class="metric-label">今日指令</div>
        <div class="metric-note">成功率 97%</div>
      </div>
      <div class="card">
        <div class="metric-value">6</div>
        <div class="metric-label">异常提醒</div>
        <div class="metric-note">需关注电量与离线</div>
      </div>
      <div class="card">
        <div class="metric-value">28ms</div>
        <div class="metric-label">平均回执</div>
        <div class="metric-note">实时链路正常</div>
      </div>
    </section>

    <section class="grid-2">
      <div class="card">
        <div class="eyebrow">设备概览</div>
        <h2>重点设备</h2>
        <ul class="data-list">
          <li>
            <div>
              <div class="item-title">DP-0008 · 小咕咕</div>
              <div class="item-subtitle">最近心跳：刚刚</div>
            </div>
            <span class="status">在线</span>
          </li>
          <li>
            <div>
              <div class="item-title">DP-0031 · 向日葵</div>
              <div class="item-subtitle">最近心跳：2 分钟前</div>
            </div>
            <span class="status">在线</span>
          </li>
          <li>
            <div>
              <div class="item-title">DP-0102 · 雾松</div>
              <div class="item-subtitle">最近心跳：12 分钟前</div>
            </div>
            <span class="status idle">离线</span>
          </li>
        </ul>
        <div style="margin-top: 16px;">
          <button class="btn subtle">查看全部设备</button>
        </div>
      </div>
      <div class="card">
        <div class="eyebrow">实时遥测</div>
        <h2>情绪与行为</h2>
        <div class="telemetry-grid">
          <div class="telemetry-item">
            <div class="telemetry-label">情绪指数</div>
            <div class="telemetry-value">0.76</div>
            <div class="telemetry-trend">+0.04</div>
          </div>
          <div class="telemetry-item">
            <div class="telemetry-label">电量</div>
            <div class="telemetry-value">83%</div>
            <div class="telemetry-trend">稳定</div>
          </div>
          <div class="telemetry-item">
            <div class="telemetry-label">互动频次</div>
            <div class="telemetry-value">14</div>
            <div class="telemetry-trend">+2</div>
          </div>
          <div class="telemetry-item">
            <div class="telemetry-label">传感器温度</div>
            <div class="telemetry-value">36.4°C</div>
            <div class="telemetry-trend">正常</div>
          </div>
        </div>
      </div>
    </section>

    <section class="grid-3">
      <div class="card">
        <div class="eyebrow">指令队列</div>
        <h3>待执行</h3>
        <p class="placeholder">队列为空，准备下发新指令。</p>
      </div>
      <div class="card">
        <div class="eyebrow">异常提醒</div>
        <h3>需处理</h3>
        <p class="placeholder">当前无高优先级异常。</p>
      </div>
      <div class="card">
        <div class="eyebrow">自动化</div>
        <h3>策略概览</h3>
        <p class="placeholder">尚未配置自动化脚本。</p>
      </div>
    </section>
  </div>
</template>
